package com.saneb.domain.announcementattachment.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.vo.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AttachmentFileRoleRulesTest {
    private static final String HASH="a".repeat(64);
    private static final String TEXT="😀 지원사업 공고\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월";
    private static AttachmentExecutionSnapshot execution(boolean current) {
        return new AttachmentExecutionSnapshot("BIZINFO_V1",HASH,"attachment-1.0.0","extractor-1.0.0",HASH,
                current?AttachmentDocumentRoleClassifier.VERSION:null,current?AttachmentDocumentRoleClassifier.RULES_HASH:null);
    }
    private static AttachmentSetEvidence.File file(String role,String origin,String quality) {
        return new AttachmentSetEvidence.File(new AttachmentSetEvidence.Locator("BIZINFO_V1","/download",Map.of("id","1")),
                "임의 이름.pdf","PDF",role,origin,"SUCCEEDED",100,HASH,null,
                new AttachmentSetEvidence.Extraction(quality,TEXT,List.of(new AttachmentSetEvidence.Block(0,0,TEXT.codePointCount(0,TEXT.length()),"p:1",true,"p:1")),1,1));
    }
    @Test void currentPolicyInfersUnknownOnlyAndBindsActualTextAndBlockHashes() {
        var input=file("UNKNOWN","UNKNOWN","COMPLETE_TEXT");
        assertThat(AttachmentFileRoleRules.selectAssessmentValid(input,execution(true))).isFalse();
        var result=AttachmentFileRoleRules.selectAssessedFile(input,execution(true));
        assertThat(result.role()).isEqualTo("NOTICE");assertThat(result.roleOrigin()).isEqualTo("TEXT_RULE");
        assertThat(result.roleAssessment().evidence()).hasSize(4);
        assertThat(AttachmentFileRoleRules.selectAssessmentValid(result,execution(true))).isTrue();
        assertThat(AttachmentFileRoleRules.selectPreservedRoleMatches("UNKNOWN","UNKNOWN",result,execution(true))).isTrue();
        assertThat(AttachmentFileRoleRules.selectPreservedRoleMatches("NOTICE","MANUAL",result,execution(true))).isFalse();
    }
    @Test void legacySnapshotsAndFilesKeepOriginalSerializationAndDoNotInfer() throws Exception {
        var json=new ObjectMapper();var legacy=execution(false);var input=file("UNKNOWN","UNKNOWN","COMPLETE_TEXT");
        assertThat(json.readTree(json.writeValueAsString(legacy)).size()).isEqualTo(5);
        assertThat(json.writeValueAsString(legacy)).doesNotContain("roleRule");
        assertThat(json.writeValueAsString(input)).doesNotContain("roleAssessment");
        assertThat(json.readValue(json.writeValueAsString(legacy),AttachmentExecutionSnapshot.class)).isEqualTo(legacy);
        assertThat(AttachmentFileRoleRules.selectAssessedFile(input,legacy)).isSameAs(input);
        assertThat(AttachmentFileRoleRules.selectAssessmentValid(input,legacy)).isTrue();
    }
    @Test void manualProfileAndIncompleteFilesAreNotOverwritten() {
        for(var input:List.of(file("NOTICE","MANUAL","COMPLETE_TEXT"),file("GUIDE","PROFILE","COMPLETE_TEXT"),
                file("UNKNOWN","UNKNOWN","PARTIAL_TEXT"))) {
            assertThat(AttachmentFileRoleRules.selectAssessedFile(input,execution(true))).isSameAs(input);
            assertThat(AttachmentFileRoleRules.selectAssessmentValid(input,execution(true))).isTrue();
            assertThat(AttachmentFileRoleRules.selectPreservedRoleMatches(input.role(),input.roleOrigin(),input,execution(true))).isTrue();
        }
    }
    @Test void completeButUncertainTextRemainsUnknownWithRecordedReason() {
        var input=file("UNKNOWN","UNKNOWN","COMPLETE_TEXT");
        var e=input.extraction();var uncertain=new AttachmentSetEvidence.Extraction(e.quality(),e.text(),
                List.of(new AttachmentSetEvidence.Block(0,0,TEXT.codePointCount(0,TEXT.length()),"p:1",false,"p:1")),1,1);
        var result=AttachmentFileRoleRules.selectAssessedFile(new AttachmentSetEvidence.File(input.locator(),input.displayName(),"PDF","UNKNOWN","UNKNOWN",
                "SUCCEEDED",100,HASH,null,uncertain),execution(true));
        assertThat(result.role()).isEqualTo("UNKNOWN");assertThat(result.roleOrigin()).isEqualTo("TEXT_RULE");
        assertThat(result.roleAssessment().reasonCode()).isEqualTo("STRUCTURE_UNCERTAIN");
        assertThat(AttachmentFileRoleRules.selectAssessmentValid(result,execution(true))).isTrue();
    }
    @Test void staleRulesAndIncompleteConfigurationPairFailClosed() {
        var stale=new AttachmentExecutionSnapshot("BIZINFO_V1",HASH,"attachment-1.0.0","extractor-1.0.0",HASH,"old",HASH);
        assertThat(stale.selectRoleRulesCurrent()).isFalse();
        assertThatIllegalArgumentException().isThrownBy(()->AttachmentFileRoleRules.selectAssessedFile(file("UNKNOWN","UNKNOWN","COMPLETE_TEXT"),stale));
        assertThatIllegalArgumentException().isThrownBy(()->new AttachmentExecutionSnapshot("BIZINFO_V1",HASH,"v1","v1",HASH,"v1",null));
        assertThatIllegalArgumentException().isThrownBy(()->new AttachmentExecutionSnapshot("BIZINFO_V1",HASH,"v1","v1",HASH,null,HASH));
    }
    @Test void forgedCheckpointEvidenceAndLegacyPolicyCannotClaimTextRuleResult() {
        var valid=AttachmentFileRoleRules.selectAssessedFile(file("UNKNOWN","UNKNOWN","COMPLETE_TEXT"),execution(true));
        var a=valid.roleAssessment();
        var forged=new AttachmentDocumentRoleClassifier.Assessment(a.ruleVersion(),a.rulesHash(),HASH,a.blocksHash(),a.roleCode(),a.reasonCode(),a.evidence());
        var invalid=new AttachmentSetEvidence.File(valid.locator(),valid.displayName(),valid.detectedType(),valid.role(),valid.roleOrigin(),
                valid.downloadStatus(),valid.downloadedBytes(),valid.binaryHash(),valid.failureCode(),valid.extraction(),forged);
        assertThat(AttachmentFileRoleRules.selectAssessmentValid(invalid,execution(true))).isFalse();
        assertThat(AttachmentFileRoleRules.selectPreservedRoleMatches("UNKNOWN","UNKNOWN",invalid,execution(true))).isFalse();
        assertThat(AttachmentFileRoleRules.selectAssessmentValid(valid,execution(false))).isFalse();
    }
}

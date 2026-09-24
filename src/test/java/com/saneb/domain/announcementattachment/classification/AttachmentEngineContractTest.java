package com.saneb.domain.announcementattachment.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AttachmentEngineContractTest {
    @Test void oldSnapshotKeepsItsExactSevenOrFiveFieldJson() throws Exception {
        var json=new ObjectMapper();
        var snapshot=new AttachmentExecutionSnapshot("BIZINFO","a".repeat(64),AnnouncementAttachmentClassificationEngine.VERSION,"1.0.3","b".repeat(64));
        assertThat(snapshot.selectEngineCurrent()).isTrue();
        assertThat(json.readTree(json.writeValueAsString(snapshot)).size()).isEqualTo(5);
        assertThat(json.writeValueAsString(snapshot)).doesNotContain("segmentRule", "roleRule");
        assertThat(json.readValue(json.writeValueAsString(snapshot),AttachmentExecutionSnapshot.class)).isEqualTo(snapshot);
        var withRole=new AttachmentExecutionSnapshot(snapshot.profileCode(),snapshot.profileHash(),snapshot.engineVersion(),
                snapshot.extractorVersion(),snapshot.extractorConfigHash(),AttachmentDocumentRoleClassifier.VERSION,AttachmentDocumentRoleClassifier.RULES_HASH);
        assertThat(json.readTree(json.writeValueAsString(withRole)).size()).isEqualTo(7);
        assertThat(json.writeValueAsString(withRole)).doesNotContain("segmentRule");
        assertThat(json.readValue(json.writeValueAsString(withRole),AttachmentExecutionSnapshot.class)).isEqualTo(withRole);
        var policy=new com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses.Configuration(
                snapshot.engineVersion(),snapshot.extractorVersion(),snapshot.extractorConfigHash(),83886080L);
        assertThat(json.readTree(json.writeValueAsString(policy)).size()).isEqualTo(4);
        assertThat(json.writeValueAsString(policy)).doesNotContain("segmentRule", "roleRule");
    }
    @Test void engineAndSegmentVersionAndHashMustBePinnedTogether() {
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH)).isTrue();
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,null,null)).isFalse();
        assertThat(AttachmentEngineContract.selectCurrent(AnnouncementAttachmentClassificationEngine.VERSION,AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH)).isFalse();
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,AttachmentSegmentRoleAnalyzer.VERSION,"0".repeat(64))).isFalse();
        assertThat(AttachmentEngineContract.selectCurrent("future",null,null)).isFalse();
    }
    @Test void incompleteSegmentIdentityCannotBeDeserializedAsReady() {
        assertThatIllegalArgumentException().isThrownBy(()->new AttachmentExecutionSnapshot("BIZINFO","a".repeat(64),AttachmentSegmentClassificationEngine.VERSION,
                "1.0.3","b".repeat(64),null,null,AttachmentSegmentRoleAnalyzer.VERSION,null));
    }
    @Test void verifiedQuarterVersionRequiresItsOwnHashAndDoesNotChangeTheDefault() {
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,
                AttachmentSegmentRoleAnalyzer.QUARTER_VERSION,AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH)).isTrue();
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,
                AttachmentSegmentRoleAnalyzer.QUARTER_VERSION,AttachmentSegmentRoleAnalyzer.RULES_HASH)).isFalse();
        assertThat(AttachmentEngineContract.selectCurrent(AttachmentSegmentClassificationEngine.VERSION,
                AttachmentSegmentRoleAnalyzer.VERSION,AttachmentSegmentRoleAnalyzer.QUARTER_RULES_HASH)).isFalse();
        assertThat(AttachmentEngineContract.selectSegmentRulesHash(AttachmentSegmentRoleAnalyzer.PARENTHESIZED_VERSION)).isNull();
        assertThat(AttachmentEngineContract.selectSegmentRulesHash(null)).isNull();
        assertThat(AttachmentSegmentRoleAnalyzer.VERSION).isEqualTo("segment-role-1.0.0");
    }
}

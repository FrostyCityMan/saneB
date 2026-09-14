package com.saneb.domain.announcementattachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.common.error.ApiException;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import com.saneb.domain.announcementattachment.vo.AttachmentFileSummaryRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttachmentReviewAssessmentTest {
    private final UUID source=UUID.randomUUID(), setId=UUID.randomUUID();
    private final ObjectMapper mapper=new ObjectMapper();
    private AttachmentEvaluationRows.Evaluation decision(String status,String reason,String warnings) {
        return new AttachmentEvaluationRows.Evaluation(UUID.randomUUID(),source,UUID.randomUUID(),setId,UUID.randomUUID(),UUID.randomUUID(),
                "v1","a".repeat(64),"b".repeat(64),status,reason,warnings,true,OffsetDateTime.now());
    }
    private AttachmentSetRow set(String discovery,boolean complete,int count,List<String> warnings) {
        return new AttachmentSetRow(setId,source,UUID.randomUUID(),UUID.randomUUID(),discovery,"SEALED","a".repeat(64),"b".repeat(64),
                count,count,complete,null,null,null,warnings);
    }
    private AttachmentFileSummaryRow file(String download,String quality,String role) {
        return new AttachmentFileSummaryRow(UUID.randomUUID(),setId,"공고문.pdf","PDF",role,"PROFILE",download,100L,"a".repeat(64),
                null,UUID.randomUUID(),quality,100,1,10,null,OffsetDateTime.now());
    }
    @Test void foundCompleteFileDoesNotRequireManualFallback() {
        var result=AttachmentReviewAssessment.select(decision("ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED","[]"),
                set("FOUND",true,1,List.of()),List.of(file("SUCCEEDED","COMPLETE_TEXT","NOTICE")),mapper);
        assertThat(result.manualSourceCheckRequired()).isFalse(); assertThat(result.requiredCodes()).isEmpty();
    }
    @Test void actualNoFilesDoesNotBecomeDiscoveryFailure() {
        var result=AttachmentReviewAssessment.select(decision("ACCEPTED","TARGET_SUPPORT_MATCH","[]"),set("NO_FILES",true,0,List.of()),List.of(),mapper);
        assertThat(result.manualSourceCheckRequired()).isFalse(); assertThat(result.requiredCodes()).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"OCR_REQUIRED","PARTIAL_TEXT","ENCRYPTED","UNSUPPORTED","FAILED"})
    void everyNonCompleteQualityRequiresManualOriginalReview(String quality) {
        var result=AttachmentReviewAssessment.select(decision("REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE","[]"),
                set("FOUND",true,1,List.of()),List.of(file("SUCCEEDED",quality,"NOTICE")),mapper);
        assertThat(result.manualSourceCheckRequired()).isTrue();
        assertThat(result.requiredCodes()).contains(quality,"ATTACHMENT_INCOMPLETE");
    }
    @Test void secondAttachmentFailureCannotBeHiddenByFirstCompleteFile() {
        var result=AttachmentReviewAssessment.select(decision("REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE","[]"),set("FOUND",true,2,List.of()),
                List.of(file("SUCCEEDED","COMPLETE_TEXT","NOTICE"),file("FAILED",null,"NOTICE")),mapper);
        assertThat(result.manualSourceCheckRequired()).isTrue();
        assertThat(result.requiredCodes()).contains("ATTACHMENT_DOWNLOAD_INCOMPLETE","ATTACHMENT_TEXT_NOT_EXTRACTED");
    }
    @ParameterizedTest @ValueSource(strings={"FAILED","PROFILE_REQUIRED","LIMIT_EXCEEDED"})
    void discoveryFailureDoesNotBecomeNoFiles(String code) {
        var result=AttachmentReviewAssessment.select(decision("REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE","[]"),set(code,false,0,List.of()),List.of(),mapper);
        assertThat(result.manualSourceCheckRequired()).isTrue(); assertThat(result.requiredCodes()).contains("ATTACHMENT_DISCOVERY_INCOMPLETE");
    }
    @Test void groupBStaysReviewAndNeedsAcknowledgementNotAutomaticExclusion() {
        var decision=decision("REVIEW_REQUIRED","ATTACHMENT_GROUP_B_MATCHED","[]");
        var result=AttachmentReviewAssessment.select(decision,set("FOUND",true,1,List.of()),List.of(file("SUCCEEDED","COMPLETE_TEXT","NOTICE")),mapper);
        assertThat(result.manualSourceCheckRequired()).isFalse(); assertThat(result.requiredCodes()).containsExactly("ATTACHMENT_GROUP_B_MATCHED");
        assertThat(decision.status()).isEqualTo("REVIEW_REQUIRED");
    }
    @Test void uncertainScopeAndUnknownRoleRequireWholeOriginal() {
        var result=AttachmentReviewAssessment.select(decision("REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW","[\"ATTACHMENT_SCOPE_UNCERTAIN\"]"),
                set("FOUND",true,1,List.of()),List.of(file("SUCCEEDED","COMPLETE_TEXT","UNKNOWN")),mapper);
        assertThat(result.manualSourceCheckRequired()).isTrue();
        assertThat(result.requiredCodes()).containsExactly("ATTACHMENT_CONTEXT_REVIEW","ATTACHMENT_ROLE_UNKNOWN","ATTACHMENT_SCOPE_UNCERTAIN");
    }
    @ParameterizedTest @ValueSource(strings={"not-json","{}","[42]","[\"not a code\"]"})
    void malformedEvidenceCannotProduceAnEmptyChecklist(String warnings) {
        assertThatThrownBy(()->AttachmentReviewAssessment.select(decision("ACCEPTED","TARGET_SUPPORT_MATCH",warnings),
                set("NO_FILES",true,0,List.of()),List.of(),mapper)).isInstanceOf(ApiException.class);
    }
}

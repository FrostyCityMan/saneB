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
    private AttachmentEvaluationRows.Evaluation segmentDecision(String status,String reason,String warnings) {
        var d=decision(status,reason,warnings);
        return new AttachmentEvaluationRows.Evaluation(d.evaluationId(),d.sourceId(),d.baseEvaluationId(),d.setId(),d.policyId(),d.ruleReleaseId(),
                "attachment-segment-1.0.0",d.inputHash(),d.decisionHash(),d.status(),d.reason(),d.warningCodesJson(),d.current(),d.evaluatedAt());
    }
    private static final String MIXED="사업 지원 안내\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월\n지원 신청서\n성 명\n(서명 또는 인)";
    private AttachmentFileSummaryRow segmentFile(String origin,String quality) {
        return new AttachmentFileSummaryRow(UUID.randomUUID(),setId,"혼합.hwpx","HWPX","UNKNOWN",origin,"SUCCEEDED",100L,"a".repeat(64),
                null,UUID.randomUUID(),quality,100,null,10,null,OffsetDateTime.now());
    }
    private AttachmentEvaluationRows.SegmentReview segmentRow(AttachmentFileSummaryRow file,String text) throws Exception {
        var blocks=new java.util.ArrayList<com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block>();
        int start=0;
        for(String line:text.split("\n")) {
            int end=start+line.codePointCount(0,line.length());
            blocks.add(new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Block(blocks.size(),start,end,"p:"+blocks.size(),true,"p:"+blocks.size()));start=end+1;
        }
        var input=new com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence.Extraction(file.qualityCode(),text,blocks,null,0);
        var analysis=new com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer().selectAnalysis(input);
        return new AttachmentEvaluationRows.SegmentReview(file.fileId(),file.extractionId(),file.qualityCode(),text,mapper.writeValueAsString(blocks),null,mapper.writeValueAsString(analysis));
    }
    @Test void resolvedMixedSegmentsRemoveOnlyTheLegacyUnknownFallback() throws Exception {
        var f=segmentFile("TEXT_RULE","COMPLETE_TEXT");var row=segmentRow(f,MIXED);
        assertThat(mapper.readTree(row.analysisJson()).path("statusCode").asText()).isEqualTo("RESOLVED");
        var result=AttachmentReviewAssessment.select(segmentDecision("ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED","[]"),set("FOUND",true,1,List.of()),List.of(f),List.of(row),mapper);
        assertThat(result.manualSourceCheckRequired()).isFalse();assertThat(result.requiredCodes()).isEmpty();
        assertThat(f.documentRoleCode()).isEqualTo("UNKNOWN");
        assertThat(AttachmentReviewAssessment.select(decision("ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED","[]"),set("FOUND",true,1,List.of()),List.of(f),List.of(row),mapper).manualSourceCheckRequired()).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"MANUAL","PROFILE",""})
    void fixedOrMissingFileRoleOriginCannotUseTheAutomaticWaiver(String origin) throws Exception {
        var f=segmentFile(origin,"COMPLETE_TEXT");
        var result=AttachmentReviewAssessment.select(segmentDecision("ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED","[]"),set("FOUND",true,1,List.of()),List.of(f),List.of(segmentRow(f,MIXED)),mapper);
        assertThat(result.manualSourceCheckRequired()).isTrue();assertThat(result.requiredCodes()).contains("ATTACHMENT_ROLE_UNKNOWN");
    }
    @Test void unknownPrefixPartialTextAndGroupBKeepTheirOwnReviewRequirements() throws Exception {
        var complete=segmentFile("TEXT_RULE","COMPLETE_TEXT");
        var unknown=AttachmentReviewAssessment.select(segmentDecision("REVIEW_REQUIRED","ATTACHMENT_CONTEXT_REVIEW","[]"),set("FOUND",true,1,List.of()),List.of(complete),List.of(segmentRow(complete,"미확인 서문\n"+MIXED)),mapper);
        assertThat(unknown.manualSourceCheckRequired()).isTrue();assertThat(unknown.requiredCodes()).contains("ATTACHMENT_ROLE_UNKNOWN");
        var partial=segmentFile("TEXT_RULE","PARTIAL_TEXT");
        var incomplete=AttachmentReviewAssessment.select(segmentDecision("REVIEW_REQUIRED","ATTACHMENT_INCOMPLETE","[]"),set("FOUND",true,2,List.of()),List.of(complete,partial),List.of(segmentRow(complete,MIXED),segmentRow(partial,MIXED)),mapper);
        assertThat(incomplete.manualSourceCheckRequired()).isTrue();assertThat(incomplete.requiredCodes()).contains("PARTIAL_TEXT","ATTACHMENT_INCOMPLETE");
        var groupB=AttachmentReviewAssessment.select(segmentDecision("REVIEW_REQUIRED","ATTACHMENT_GROUP_B_MATCHED","[]"),set("FOUND",true,1,List.of()),List.of(complete),List.of(segmentRow(complete,MIXED)),mapper);
        assertThat(groupB.manualSourceCheckRequired()).isFalse();assertThat(groupB.requiredCodes()).containsExactly("ATTACHMENT_GROUP_B_MATCHED");
    }
    @Test void missingForeignDuplicateOrTamperedBoundAnalysisCannotReduceReview() throws Exception {
        var f=segmentFile("TEXT_RULE","COMPLETE_TEXT");var row=segmentRow(f,MIXED);
        var d=segmentDecision("ACCEPTED","EXTENDED_TARGET_SUPPORT_CONFIRMED","[]");var s=set("FOUND",true,1,List.of());
        for(var rows:List.of(List.<AttachmentEvaluationRows.SegmentReview>of(),List.of(row,row),
                List.of(new AttachmentEvaluationRows.SegmentReview(UUID.randomUUID(),row.extractionId(),row.qualityCode(),row.extractedText(),row.blocksJson(),null,row.analysisJson())),
                List.of(new AttachmentEvaluationRows.SegmentReview(row.fileId(),UUID.randomUUID(),row.qualityCode(),row.extractedText(),row.blocksJson(),null,row.analysisJson())),
                List.of(new AttachmentEvaluationRows.SegmentReview(row.fileId(),row.extractionId(),row.qualityCode(),row.extractedText()+"변경",row.blocksJson(),null,row.analysisJson())),
                List.of(new AttachmentEvaluationRows.SegmentReview(row.fileId(),row.extractionId(),row.qualityCode(),row.extractedText(),row.blocksJson(),null,null)),
                List.of(new AttachmentEvaluationRows.SegmentReview(row.fileId(),row.extractionId(),row.qualityCode(),row.extractedText(),row.blocksJson(),null,"{}")))) {
            assertThatThrownBy(()->AttachmentReviewAssessment.select(d,s,List.of(f),rows,mapper)).isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(()->AttachmentReviewAssessment.select(d,s,List.of(f),mapper)).isInstanceOf(ApiException.class);
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

package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 실사이트 성공이 아니라 고정 두 형식·명시 구간 버전·저장 증거의 검증기 계약이다. */
class AnnouncementAttachmentJungguWorkerProbeTest {
    static final String MODE="JUNGGU_SEGMENT";
    static final List<String> FLAGS=List.of("segmentEvaluationInputBound","segmentApiProjectionMatched","legacyDefaultReadOnlyVerified",
            "evaluationBoundApiVerified","otherVersionReadOnlyVerified","pinnedInputAndCoverageVerified");
    @Test void fixedTwoFileCaseUsesExplicitVersionAndDoesNotExpandOtherGroups() {
        assertEquals(List.of("JUNGGU-33626"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(MODE));
        assertEquals("JUNGGU_PDF",AnnouncementAttachmentOfficialWorkerProbe.selectObservationGroup(MODE));
        var samples=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectFixedCases(MODE).toList();
        assertEquals(1,samples.size());assertEquals(2,samples.getFirst().listedFileCount());
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected("JUNGGU-33626"));
        assertEquals(2,AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("JUNGGU-33626"));
        assertEquals(5,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests(MODE));
        assertEquals(25165824,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes(MODE));
        var execution=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(MODE,samples.getFirst(),"1.0.14","b".repeat(64));
        assertEquals(AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION,execution.segmentRuleVersion());
        assertEquals(AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH,execution.segmentRulesHash());assertTrue(execution.selectEngineCurrent());
        assertEquals(25165824,AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPolicyConfiguration(execution).maximumSourceBytes());
        assertEquals(44,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests("BOEUN"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,2,2,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,1,0,0,1,0,0));
        // 기존30회에서 남은3회로 실행할 수 없다. 최대5회 실행에는32회로 범위 승인 필요.
        assertTrue(27+AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests(MODE)>30);
    }
    @Test void bothFormatsAndStoredEvidenceAreRequiredWithoutApprovingNormalCandidate() {
        assertTrue(valid(report()));
        for(String key:List.of("bodyStageComplete","discoveryComplete","segmentDatabaseApiVerified","segmentReviewContextVerified","manualSourceCheckRequired","requiresFinalAdminVerification")) {
            assertFalse(valid(report().put(key,false)),key);assertFalse(valid(report().put(key,"true")),key);
        }
        for(String key:List.of("isWholeTextAnalysisComplete","isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e"))
            assertFalse(valid(report().put(key,true)),key);
        for(String key:List.of("caseCode","profileCode","profileHash","engineVersion","segmentRuleVersion","segmentRulesHash","extractorVersion","workerStatus","bodyStatus","decisionStatus","decisionReason"))
            assertFalse(valid(report().put(key,"changed")),key);
        for(String key:List.of("productionWriteCount","maximumRequestReservations","maximumReservedBytes","requestReservationsIncludingBodyUpperBound","discoveredFileCount","processedFileCount","extractorCalls"))
            assertFalse(valid(report().put(key,report().path(key).asLong()+1)),key);
        var invalid=report();invalid.withArray("files").remove(0);assertFalse(valid(invalid));
        invalid=report();invalid.withArray("files").add(invalid.at("/files/0").deepCopy());assertFalse(valid(invalid));
        invalid=report();var reversed=invalid.putArray("files");reversed.add(report().at("/files/1")).add(report().at("/files/0"));assertTrue(valid(invalid));
    }
    @Test void partialPdfCannotBePromotedAndEitherFileMismatchFails() {
        for(int index=0;index<2;index++) {
            for(String key:FLAGS) {
                var invalid=report();file(invalid,index).remove(key);assertFalse(valid(invalid),key);
                invalid=report();file(invalid,index).put(key,"true");assertFalse(valid(invalid),key);
            }
            for(String key:List.of("binaryHash","textHash","format","quality","downloadStatus","segmentAnalysisHash")) {
                var invalid=report();file(invalid,index).put(key,"changed");assertFalse(valid(invalid),key);
            }
            for(String key:List.of("bytes","characterCount","blockCount")) {
                var invalid=report();file(invalid,index).put(key,file(invalid,index).path(key).asLong()+1);assertFalse(valid(invalid),key);
            }
            for(String key:List.of("longFormObservedHashMatched","longFormCandidate","structuralCandidate")) {
                var invalid=report();file(invalid,index).put(key,true);assertFalse(valid(invalid),key);
            }
        }
        for(String key:List.of("segmentCount","unknownSegmentCount","noticeSegmentCount")) {
            var invalid=report();file(invalid,1).put(key,file(invalid,1).path(key).asLong()+1);assertFalse(valid(invalid),key);
        }
        var invalid=report();file(invalid,1).put("partialFullCoverageVerified",false);assertFalse(valid(invalid));
        invalid=report();file(invalid,1).put("segmentReason","ROLE_TEXT_STRUCTURE_MATCHED");assertFalse(valid(invalid));
    }
    private boolean valid(ObjectNode report){return AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(MODE,report);}
    private ObjectNode file(ObjectNode report,int index){return (ObjectNode)report.at("/files/"+index);}
    private ObjectNode report() {
        var report=new ObjectMapper().createObjectNode().put("caseCode","JUNGGU-33626").put("profileCode","LOCAL_DAEGU_JUNGGU_GET_V1")
                .put("profileHash","e648e332e85e22fd2a6818eaf48b1a5d7ae58d4cad50b9e9ee0da847d73541ef")
                .put("engineVersion","attachment-segment-1.0.0").put("segmentRuleVersion",AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION)
                .put("segmentRulesHash",AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH).put("extractorVersion","1.0.14")
                .put("workerStatus","EVALUATED").put("bodyStatus","AVAILABLE").put("decisionStatus","REVIEW_REQUIRED").put("decisionReason","ATTACHMENT_INCOMPLETE")
                .put("discoveredFileCount",2).put("processedFileCount",2).put("extractorCalls",2).put("productionWriteCount",0)
                .put("maximumRequestReservations",5).put("maximumReservedBytes",25165824).put("requestReservationsIncludingBodyUpperBound",5).put("reservedBytesIncludingBodyUpperBound",2439945);
        for(String key:List.of("bodyStageComplete","discoveryComplete","segmentDatabaseApiVerified","segmentReviewContextVerified","manualSourceCheckRequired","requiresFinalAdminVerification"))report.put(key,true);
        for(String key:List.of("isWholeTextAnalysisComplete","isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e"))report.put(key,false);
        var files=report.putArray("files");
        for(boolean pdf:List.of(false,true)) {
            var file=files.addObject().put("format",pdf?"PDF":"HWP").put("quality",pdf?"PARTIAL_TEXT":"COMPLETE_TEXT").put("downloadStatus","SUCCEEDED")
                    .put("binaryHash",pdf?"6a57302609860d5332ccf95650cd754999270d6e6308acae19f06bd74104326c":"ae74fb4881522239bcb91a6f64dc137e1af55a207050ecfdc7e5a01f2a7026d6")
                    .put("textHash",pdf?"9f6ed99df2287fe3e4b1e5546fcd44a045eb6c7434aaa3a65566e8b0aa2938cf":"76a66ee11c3a7e729f0777e19b7b17fef9de708ce893a199b381f603ca5a45bd")
                    .put("bytes",pdf?209769:127488).put("characterCount",pdf?4241:3120).put("blockCount",pdf?5:178)
                    .put("segmentAnalysisHash","a".repeat(64)).put("segmentCount",pdf?1:4).put("unknownSegmentCount",1).put("noticeSegmentCount",0);
            FLAGS.forEach(key->file.put(key,true));
            if(pdf)file.put("partialFullCoverageVerified",true).put("segmentReason","COMPLETE_TEXT_REQUIRED");
        }
        return report;
    }
}

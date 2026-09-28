package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 외부 요청·DB 없이 고정 실행 범위와 부분 추출 보고서 위조를 검사한다. */
class AnnouncementAttachmentHamanWorkerProbeTest {
    private static final String MODE="HAMAN_SEGMENT";
    private static final List<String> FLAGS=List.of("segmentEvaluationInputBound","segmentApiProjectionMatched",
            "legacyDefaultReadOnlyVerified","evaluationBoundApiVerified","otherVersionReadOnlyVerified","partialFullCoverageVerified");

    @Test void fixedSingleHamanCasePinsVersionAndRetainsExistingBudgets() {
        assertEquals(List.of("HAMAN-41306"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(MODE));
        assertEquals("HAMAN",AnnouncementAttachmentOfficialWorkerProbe.selectObservationGroup(MODE));
        var samples=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectFixedCases(MODE).toList();
        assertEquals(1,samples.size());assertEquals("HAMAN-41306",samples.getFirst().code());
        assertEquals(1,samples.getFirst().listedFileCount());
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected("HAMAN-41306"));
        assertEquals(1,AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount("HAMAN-41306"));
        assertEquals(5,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests(MODE));
        assertEquals(25165824L,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes(MODE));
        var execution=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(MODE,samples.getFirst(),"1.0.12","b".repeat(64));
        assertEquals(AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION,execution.segmentRuleVersion());
        assertEquals(AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH,execution.segmentRulesHash());assertTrue(execution.selectEngineCurrent());
        var policy=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPolicyConfiguration(execution);
        assertEquals(execution.segmentRulesHash(),policy.segmentRulesHash());assertEquals(25165824L,policy.maximumSourceBytes());
        assertEquals(44,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests("BOEUN"));
        assertEquals("segment-role-1.0.2",AnnouncementAttachmentOfficialWorkerProbe.selectSegmentVersion("BOEUN_SEGMENT"));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,1,0,0,1,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,2,2,0,0,0,0));
        var other=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectFixedCases("BOEUN").findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(MODE,other,"1.0.12","b".repeat(64)));
    }

    @Test void partialStorageEvidenceIsNotACompleteFileOrApprovedExpectation() {
        var report=report();assertTrue(valid(report));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete("BOEUN_LONG_FORM",report));
        for(String key:List.of("isWholeTextAnalysisComplete","isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e")) {
            assertFalse(valid(report.deepCopy().put(key,true)),key);
            assertFalse(valid(report.deepCopy().put(key,"false")),key);
            var missing=report.deepCopy();missing.remove(key);assertFalse(valid(missing),key);
        }
        for(String key:List.of("segmentDatabaseApiVerified","segmentReviewContextVerified","manualSourceCheckRequired","requiresFinalAdminVerification","bodyStageComplete","discoveryComplete"))
            assertFalse(valid(report.deepCopy().put(key,false)),key);
        for(String key:FLAGS) {
            var invalid=report.deepCopy();file(invalid).put(key,"true");assertFalse(valid(invalid),key);
            invalid=report.deepCopy();file(invalid).remove(key);assertFalse(valid(invalid),key);
        }
        assertFalse(valid(report.deepCopy().put("decisionStatus","ACCEPTED")));
        assertFalse(valid(report.deepCopy().put("extractorVersion","1.0.11")));
        assertFalse(valid(report.deepCopy().put("segmentRuleVersion","segment-role-1.0.3")));
        assertFalse(valid(report.deepCopy().put("segmentRulesHash","a".repeat(64))));
        assertFalse(valid(report.deepCopy().put("bodyStatus","UNAVAILABLE")));
        assertFalse(valid(report.deepCopy().put("caseCode","HAMAN-41307")));
        var invalid=report.deepCopy();file(invalid).put("quality","COMPLETE_TEXT");assertFalse(valid(invalid));
        invalid=report.deepCopy();file(invalid).put("segmentReason","ROLE_TEXT_STRUCTURE_MATCHED");assertFalse(valid(invalid));
        for(String key:List.of("longFormObservedHashMatched","longFormCandidate","structuralCandidate")) {
            invalid=report.deepCopy();file(invalid).put(key,true);assertFalse(valid(invalid),key);
        }
    }

    @Test void alteredFilesCountsAndBudgetsCannotPass() {
        var report=report();
        for(String key:List.of("bytes","characterCount","blockCount","segmentCount","unknownSegmentCount","noticeSegmentCount")) {
            var invalid=report.deepCopy();file(invalid).put(key,file(report).path(key).asLong()+1);assertFalse(valid(invalid),key);
            invalid=report.deepCopy();file(invalid).put(key,file(report).path(key).asText());assertFalse(valid(invalid),key);
        }
        for(String key:List.of("binaryHash","textHash","segmentAnalysisHash")) {
            var invalid=report.deepCopy();file(invalid).put(key,"invalid");assertFalse(valid(invalid),key);
        }
        for(String key:List.of("productionWriteCount","discoveredFileCount","processedFileCount","maximumRequestReservations","maximumReservedBytes"))
            assertFalse(valid(report.deepCopy().put(key,report.path(key).asLong()+1)),key);
        assertFalse(valid(report.deepCopy().put("requestReservationsIncludingBodyUpperBound",6)));
        assertFalse(valid(report.deepCopy().put("reservedBytesIncludingBodyUpperBound",25165825)));
        var invalid=report.deepCopy();invalid.withArray("files").add(file(report).deepCopy());assertFalse(valid(invalid));
        invalid=report.deepCopy();invalid.withArray("files").removeAll();assertFalse(valid(invalid));
    }
    private boolean valid(ObjectNode value){return AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(MODE,value);}
    @Test void layoutModePinsNewInputBeforeRequestsAndKeepsHistoricalReportSeparate() {
        String mode="HAMAN_LAYOUT_SEGMENT";
        assertEquals(List.of("HAMAN-41306"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(mode));
        var sample=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectFixedCases(mode).findFirst().orElseThrow();
        assertEquals(5,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests(mode));
        assertEquals(25165824,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes(mode));
        var execution=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(mode,sample,"1.0.15","b".repeat(64));
        assertEquals(AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION,execution.segmentRuleVersion());
        for(String version:List.of("1.0.12","1.0.14","1.0.16"))
            assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(mode,sample,version,"b".repeat(64)));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,layoutReport()));
        assertFalse(valid(layoutReport()));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,report()));
    }
    @Test void layoutReportRejectsChangedPartialCausesAndPromotionClaims() {
        String mode="HAMAN_LAYOUT_SEGMENT";
        for(String key:List.of("isWholeTextAnalysisComplete","isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e"))
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,layoutReport().put(key,true)),key);
        for(String key:List.of("characterCount","blockCount","textHash","quality","partialFullCoverageVerified")) {
            var invalid=layoutReport();file(invalid).remove(key);
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,invalid),key);
        }
        for(String field:List.of("count","code")) {
            var invalid=layoutReport();((ObjectNode)file(invalid).path("hwpPartialCauses").get(1)).put(field,"1");
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,invalid),field);
        }
        for(int count:List.of(0,2)) {
            var invalid=layoutReport();((ObjectNode)file(invalid).path("hwpPartialCauses").get(1)).put("count",count);
            assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,invalid));
        }
        var invalid=layoutReport();file(invalid).remove("hwpPartialCauses");
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(mode,invalid));
    }
    private ObjectNode layoutReport() {
        var report=report().put("extractorVersion","1.0.15").put("profileCode","LOCAL_HAMAN_GET_V1")
                .put("profileHash","c7cb4961b49e97449fe09afb77c5a4d009d3bf2df93d611c6c547046ad926513")
                .put("workerStatus","EVALUATED").put("decisionReason","ATTACHMENT_INCOMPLETE").put("extractorCalls",1);
        var file=file(report).put("characterCount",4647).put("blockCount",214).put("downloadStatus","SUCCEEDED")
                .put("textHash","88bb6aebc74813186f8d9f3ac32b457e4a674c98bbf3649e3a1435077435542a");
        var causes=file.putArray("hwpPartialCauses");
        causes.addObject().put("code","UNSUPPORTED_RECORD").put("count",2);
        causes.addObject().put("code","UNSUPPORTED_CONTROL").put("count",1);
        return report;
    }
    private ObjectNode file(ObjectNode value){return (ObjectNode)value.path("files").get(0);}
    private ObjectNode report() {
        var report=new ObjectMapper().createObjectNode().put("caseCode","HAMAN-41306").put("engineVersion","attachment-segment-1.0.0")
                .put("segmentRuleVersion",AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION).put("segmentRulesHash",AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH)
                .put("extractorVersion","1.0.12").put("decisionStatus","REVIEW_REQUIRED").put("bodyStatus","AVAILABLE")
                .put("bodyStageComplete",true).put("discoveryComplete",true).put("discoveredFileCount",1).put("processedFileCount",1)
                .put("segmentDatabaseApiVerified",true).put("segmentReviewContextVerified",true).put("manualSourceCheckRequired",true)
                .put("requiresFinalAdminVerification",true).put("isWholeTextAnalysisComplete",false).put("isPolicyQaPassed",false)
                .put("isExpectationApproved",false).put("isAuthenticatedBrowserE2e",false).put("productionWriteCount",0)
                .put("maximumRequestReservations",5).put("maximumReservedBytes",25165824)
                .put("requestReservationsIncludingBodyUpperBound",4).put("reservedBytesIncludingBodyUpperBound",2204906);
        var file=report.putArray("files").addObject().put("format","HWP").put("quality","PARTIAL_TEXT")
                .put("binaryHash","c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97")
                .put("textHash","ea24e32e9c049cf8a2a7d3ffae300ffdee864ac33939a78ef520cbfa334fb5f7")
                .put("bytes",101888).put("characterCount",4644).put("blockCount",213).put("segmentAnalysisHash","b".repeat(64))
                .put("segmentCount",1).put("unknownSegmentCount",1).put("noticeSegmentCount",0).put("segmentReason","COMPLETE_TEXT_REQUIRED");
        FLAGS.forEach(key->file.put(key,true));return report;
    }
}

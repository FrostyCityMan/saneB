package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saneb.domain.announcementattachment.classification.AttachmentSegmentRoleAnalyzer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentHwacheonWorkerProbeTest {
    static final String MODE="HWACHEON_SEGMENT";
    @Test void fixedCaseVersionBudgetAndTitleDoNotExpandOtherGroups() {
        assertEquals(List.of("HWACHEON-32258"),AnnouncementAttachmentOfficialWorkerProbe.selectCaseCodes(MODE));
        var sample=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectFixedCases(MODE).findFirst().orElseThrow();
        assertEquals(1,sample.listedFileCount());assertEquals("LGS-000130",sample.source().localSourceCode());
        assertEquals("SAFE_SAEOL_EMINWON_LEGACY",sample.source().listParserProfileCode());
        assertEquals("HWACHEON",AnnouncementAttachmentOfficialWorkerProbe.selectObservationGroup(MODE));
        assertEquals(5,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumRequests(MODE));
        assertEquals(25165824,AnnouncementAttachmentOfficialWorkerProbe.selectMaximumBytes(MODE));
        assertEquals(1,AnnouncementAttachmentOfficialWorkerProbe.selectExpectedExtractionCount(sample.code()));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(sample.code()));
        assertTrue(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,1,1,0,0,0,0));
        assertFalse(AnnouncementAttachmentOfficialWorkerProbe.selectComplete(MODE,1,0,0,1,0,0));
        assertEquals("segment-role-1.0.4",AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(MODE,sample,"1.0.15","b".repeat(64)).segmentRuleVersion());
        for(String version:List.of("1.0.12","1.0.14","1.0.16"))assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentOfficialWorkerProbe.validatePinnedExtractor(MODE,version));
        assertDoesNotThrow(()->HwacheonOfficialWorkerContract.validateBinary(82944,HwacheonOfficialWorkerContract.BINARY));
        assertThrows(java.io.IOException.class,()->HwacheonOfficialWorkerContract.validateBinary(82945,HwacheonOfficialWorkerContract.BINARY));
        assertThrows(java.io.IOException.class,()->HwacheonOfficialWorkerContract.validateBinary(82944,"a".repeat(64)));
        String page="<form name='form1' method='post'><table width='100%' border='0' cellspacing='1' cellpadding='0'><tr><th>제목</th><td>"+sample.title()+"</td></tr></table></form>";
        AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(page),sample.title(),sample.titleLayout());
        for(String invalid:List.of(page+page,page.replace("<th>제목</th>","<td>제목</td>"),page.replace(sample.title(),"다른 공고"),page.replace("</td>","<input name='other'></td>")))
            assertThrows(AssertionError.class,()->AnnouncementAttachmentBbsOfficialObservationTest.validateTitle(Jsoup.parse(invalid),sample.title(),sample.titleLayout()));
    }
    @Test void observationAllowsMeasuredPartialQualityButNotFalsePromotion() {
        var report=report();assertTrue(valid(report));
        for(String key:List.of("isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e","isWholeTextAnalysisComplete")) {
            assertFalse(valid(report.deepCopy().put(key,true)),key);assertFalse(valid(report.deepCopy().put(key,"false")),key);
        }
        for(String key:List.of("manualSourceCheckRequired","bodyStageComplete","discoveryComplete","originalFilesRemoved","segmentDatabaseApiVerified","segmentReviewContextVerified","requiresFinalAdminVerification"))
            assertFalse(valid(report.deepCopy().put(key,false)),key);
        for(String key:HwacheonOfficialWorkerContract.FILE_FLAGS){var changed=report.deepCopy();file(changed).remove(key);assertFalse(valid(changed),key);}
        assertFalse(valid(report.deepCopy().put("decisionStatus","ACCEPTED")));
        var complete=report.deepCopy().put("isWholeTextAnalysisComplete",true);file(complete).put("quality","COMPLETE_TEXT");
        assertTrue(valid(complete)); // 완전 텍스트여도 UNKNOWN은 검수 유지, 기대값/정책 승인 아님.
        assertFalse(valid(complete.put("manualSourceCheckRequired",false)));
    }
    @Test void changedIdentityCountsAndMissingEvidenceFailClosed() {
        for(String key:List.of("caseCode","profileHash","scope","status","extractorVersion","segmentRuleVersion","segmentRulesHash","titleStage","workerStatus"))
            assertFalse(valid(report().put(key,"changed")),key);
        for(String key:List.of("productionWriteCount","remainingResourceLeases","discoveredFileCount","processedFileCount","extractorCalls","maximumRequestReservations","maximumReservedBytes"))
            assertFalse(valid(report().put(key,report().path(key).asLong()+1)),key);
        for(String key:List.of("binaryHash","locatorHash","textHash","segmentAnalysisHash","bytes","characterCount","blockCount","segmentCount")) {
            var changed=report();file(changed).put(key,"invalid");assertFalse(valid(changed),key);
        }
        for(String key:List.of("longFormObservedHashMatched","longFormCandidate","structuralCandidate")){var changed=report();file(changed).put(key,true);assertFalse(valid(changed));}
        assertFalse(valid(report().put("requestReservationsIncludingBodyUpperBound",6)));
        assertFalse(valid(report().put("reservedBytesIncludingBodyUpperBound",25165825)));
        var extra=report();extra.withArray("files").add(file(extra).deepCopy());assertFalse(valid(extra));
        var missing=report();missing.withArray("files").removeAll();assertFalse(valid(missing));
    }
    static boolean valid(ObjectNode r){return AnnouncementAttachmentOfficialWorkerProbe.selectSegmentReportComplete(MODE,r);}
    static ObjectNode file(ObjectNode r){return (ObjectNode)r.path("files").get(0);}
    static ObjectNode report(){
        var r=new ObjectMapper().createObjectNode().put("caseCode","HWACHEON-32258").put("scope","OFFICIAL_WORKER_EPHEMERAL_DB_API_V1")
                .put("status","WORKER_DB_API_OBSERVED_NOT_APPROVED").put("profileCode","LOCAL_HWACHEON_POST_V1").put("profileHash",HwacheonOfficialWorkerContract.PROFILE)
                .put("engineVersion","attachment-segment-1.0.0").put("segmentRuleVersion",AttachmentSegmentRoleAnalyzer.LONG_FORM_VERSION)
                .put("segmentRulesHash",AttachmentSegmentRoleAnalyzer.LONG_FORM_RULES_HASH).put("extractorVersion","1.0.15")
                .put("workerStatus","EVALUATED").put("titleStage","COMBINATION_MATCHED").put("bodyStatus","AVAILABLE").put("decisionStatus","REVIEW_REQUIRED");
        for(String key:List.of("isPolicyQaPassed","isExpectationApproved","isAuthenticatedBrowserE2e","isWholeTextAnalysisComplete"))r.put(key,false);
        for(String key:List.of("bodyStageComplete","discoveryComplete","originalFilesRemoved","segmentDatabaseApiVerified","segmentReviewContextVerified","requiresFinalAdminVerification","manualSourceCheckRequired"))r.put(key,true);
        r.put("productionWriteCount",0).put("remainingResourceLeases",0).put("discoveredFileCount",1).put("processedFileCount",1).put("extractorCalls",1)
                .put("maximumRequestReservations",5).put("maximumReservedBytes",25165824).put("requestReservationsIncludingBodyUpperBound",4).put("reservedBytesIncludingBodyUpperBound",2200000);
        var f=r.putArray("files").addObject().put("format","HWP").put("downloadStatus","SUCCEEDED").put("binaryHash",HwacheonOfficialWorkerContract.BINARY)
                .put("locatorHash",HwacheonOfficialWorkerContract.LOCATOR).put("bytes",82944).put("quality","PARTIAL_TEXT")
                .put("characterCount",100).put("blockCount",2).put("textHash","a".repeat(64)).put("segmentAnalysisHash","b".repeat(64))
                .put("segmentCount",1).put("unknownSegmentCount",1).put("noticeSegmentCount",0).put("partialFullCoverageVerified",true).put("segmentReason","COMPLETE_TEXT_REQUIRED");
        for(String key:HwacheonOfficialWorkerContract.FILE_FLAGS)f.put(key,true);return r;
    }
}

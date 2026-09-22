package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentBbsObservationProbeTest {
    private static final Instant START = Instant.parse("2026-09-22T01:00:00Z");
    private List<JsonNode> okcheonReports() {
        var reports = new ArrayList<JsonNode>();
        for (String code : AnnouncementAttachmentBbsObservationProbe.OKCHEON_CASES) {
            var node = report();
            node.put("caseCode", code).put("profileCode", "LOCAL_OKCHEON_BBS_V1")
                    .put("observedAt", START.plusSeconds(1).toString()).put("expectedListedFileCount", 1);
            node.remove("files");
            var files = node.putArray("files");
            if (code.equals("OKCHEON-193187")) {
                node.put("status", "TITLE_NOT_ELIGIBLE_NOT_FETCHED").put("titleStage", "COMBINATION_NOT_MATCHED")
                        .put("titleReason", "TITLE_COMBINATION_NOT_MATCHED").put("isWholeTextAnalysisComplete", false)
                        .put("requiresFinalAdminVerification", false).put("requestReservationsIncludingBodyUpperBound", 0)
                        .put("reservedBytesIncludingBodyUpperBound", 0);
                node.remove("bodyStageComplete");
            } else {
                node.put("titleStage", "COMBINATION_MATCHED").put("bodyStatus", "AVAILABLE")
                        .put("discoveryStatus", "FOUND").put("discoveryComplete", true).put("discoveredFileCount", 1)
                        .put("decisionStatus", "REVIEW_REQUIRED").put("isWholeTextAnalysisComplete", true);
                files.addObject().put("status", "OBSERVED").put("format", "HWPX").put("bytes", 1024)
                        .put("quality", "COMPLETE_TEXT").put("characterCount", 1000).put("blockCount", 30)
                        .putObject("roleAssessment").put("roleCode", "UNKNOWN");
            }
            reports.add(node);
        }
        return reports;
    }
    private boolean validOkcheon(List<JsonNode> reports) {
        return AnnouncementAttachmentBbsObservationProbe.selectOkcheonReportsComplete(reports, START, START.plusSeconds(60));
    }
    @Test void onlyExplicitModesCanChooseFixedScope() {
        String hash = "a".repeat(64);
        assertEquals("OBSERVATION", AnnouncementAttachmentBbsObservationProbe.selectMode(new String[]{hash}));
        for (String mode : List.of("FIXED", "OKCHEON"))
            assertEquals(mode, AnnouncementAttachmentBbsObservationProbe.selectMode(new String[]{hash, mode}));
        for (String[] args : new String[][]{{}, {"bad"}, {hash,"OTHER"}, {hash,"TAEBAEK_HWP"}, {hash,"OKCHEON","extra"}})
            assertThrows(IllegalArgumentException.class, () -> AnnouncementAttachmentBbsObservationProbe.selectMode(args));
    }
    @Test void okcheonRequiresAllThreeTestsIncludingNegativeSample() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectOkcheonComplete(3,3,0,0,0,0));
        for (long[] v : new long[][]{{0,0,0,0,0,0},{2,2,0,0,0,0},{3,2,1,0,0,0},{3,2,0,1,0,0},
                {3,2,0,0,1,0},{3,3,0,0,0,1},{4,4,0,0,0,0}})
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectOkcheonComplete(v[0],v[1],v[2],v[3],v[4],v[5]));
    }
    @Test void temporaryScopeMatchesActualJUnitCaseSelection() {
        var cases = AnnouncementAttachmentBbsOfficialObservationTest.selectCases("OKCHEON").toList();
        assertEquals(AnnouncementAttachmentBbsObservationProbe.OKCHEON_CASES, cases.stream().map(c -> c.code()).toList());
        assertNull(cases.get(0).expectedTitleStopStage());
        assertNull(cases.get(1).expectedTitleStopStage());
        assertEquals("COMBINATION_NOT_MATCHED", cases.get(2).expectedTitleStopStage().name());
        assertTrue(cases.stream().allMatch(c -> c.listedFileCount() == 1
                && c.profile().selectProfileCode().equals("LOCAL_OKCHEON_BBS_V1")));
    }
    @Test void okcheonObservationsNeverApprovePolicyAndRetainIncompleteText() {
        assertTrue(validOkcheon(okcheonReports()));
        for (String quality : List.of("PARTIAL_TEXT", "OCR_REQUIRED", "ENCRYPTED", "CORRUPT", "UNSUPPORTED", "LIMIT_EXCEEDED")) {
            var reports = okcheonReports();
            ((ObjectNode)reports.getFirst().at("/files/0")).put("quality", quality);
            assertFalse(validOkcheon(reports));
            ((ObjectNode)reports.getFirst()).put("isWholeTextAnalysisComplete", false);
            assertTrue(validOkcheon(reports), quality);
        }
        for (String key : List.of("isPolicyQaPassed", "isExpectationApproved")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, true);
            assertFalse(validOkcheon(reports));
        }
    }
    @Test void okcheonRejectsMissingDuplicateAndChangedCaseOrProfile() {
        assertFalse(validOkcheon(null));
        var reports = okcheonReports(); reports.removeLast(); assertFalse(validOkcheon(reports));
        reports = okcheonReports(); reports.set(1, reports.getFirst()); assertFalse(validOkcheon(reports));
        reports = okcheonReports(); reports.set(1, null); assertFalse(validOkcheon(reports));
        for (String key : List.of("caseCode", "profileCode", "scope")) {
            reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, "OTHER"); assertFalse(validOkcheon(reports));
        }
    }
    @Test void okcheonRejectsStaleFutureAndMissingTimestamp() {
        for (String timestamp : List.of(START.minusSeconds(1).toString(), START.plusSeconds(61).toString(), "invalid", "")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put("observedAt", timestamp);
            assertFalse(validOkcheon(reports));
        }
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectOkcheonReportsComplete(okcheonReports(), null, START));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectOkcheonReportsComplete(okcheonReports(), START, START.minusSeconds(1)));
    }
    @Test void okcheonRejectsChangedBudgetsOperatingWritesAndCoercedTypes() {
        for (String key : List.of("expectedListedFileCount", "discoveredFileCount", "maximumRequestReservations",
                "maximumReservedBytes", "requestReservationsIncludingBodyUpperBound", "reservedBytesIncludingBodyUpperBound", "productionWriteCount")) {
            for (long value : new long[]{-1, 251658241}) {
                var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, value);
                assertFalse(validOkcheon(reports), key);
            }
            var reports = okcheonReports(); var node = (ObjectNode)reports.getFirst(); node.put(key, node.path(key).asText());
            assertFalse(validOkcheon(reports), key);
        }
        for (String key : List.of("originalFilesRemoved", "requiresFinalAdminVerification", "bodyStageComplete", "discoveryComplete")) {
            var reports = okcheonReports(); var node = (ObjectNode)reports.getFirst(); node.put(key, false);
            assertFalse(validOkcheon(reports)); node.put(key, "true"); assertFalse(validOkcheon(reports));
        }
    }
    @Test void negativeTitleMustNeverFetchBodyOrFiles() {
        for (String key : List.of("requestReservationsIncludingBodyUpperBound", "reservedBytesIncludingBodyUpperBound")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getLast()).put(key, 1); assertFalse(validOkcheon(reports));
        }
        for (String key : List.of("bodyStatus", "discoveryStatus", "titleReason", "status", "titleStage")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getLast()).put(key, "CHANGED"); assertFalse(validOkcheon(reports));
        }
        var reports = okcheonReports(); ((ObjectNode)reports.getLast()).withArray("files").addObject(); assertFalse(validOkcheon(reports));
        for (String key : List.of("isWholeTextAnalysisComplete", "requiresFinalAdminVerification")) {
            reports = okcheonReports(); ((ObjectNode)reports.getLast()).put(key, true); assertFalse(validOkcheon(reports));
        }
    }
    @Test void positiveTitlesRequireBodyWholeFileAndRoleEvidence() {
        for (String key : List.of("status", "titleStage", "bodyStatus", "discoveryStatus", "decisionStatus")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).put(key, "FAILED"); assertFalse(validOkcheon(reports));
        }
        for (String key : List.of("status", "format", "quality")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst().at("/files/0")).put(key, "FAILED"); assertFalse(validOkcheon(reports));
        }
        for (String key : List.of("bytes", "characterCount", "blockCount")) {
            var reports = okcheonReports(); ((ObjectNode)reports.getFirst().at("/files/0")).put(key, 0); assertFalse(validOkcheon(reports));
        }
        var reports = okcheonReports(); ((ObjectNode)reports.getFirst()).withArray("files").removeAll(); assertFalse(validOkcheon(reports));
        reports = okcheonReports(); ((ObjectNode)reports.getFirst().at("/files/0")).remove("roleAssessment"); assertFalse(validOkcheon(reports));
    }
    private ObjectNode fixedReport() {
        var node=new ObjectMapper().createObjectNode();
        node.put("caseCode","TAEBAEK-184816").put("scope","FIXED_CASE_EXECUTOR_QA_EPHEMERAL_ONLY")
                .put("status","FIXED_EXPECTATIONS_MATCHED_REVIEW_REQUIRED").put("productionWriteCount",0)
                .put("originalFilesRemoved",true).put("isPolicyQaPassed",false)
                .put("isExpectationCoverageComplete",false).put("normalNotice",false);
        var result=node.putObject("result");
        result.put("scope","SINGLE_FIXED_NOTICE_PROVIDER_QA").put("caseId","TAEBAEK-184816")
                .put("status","PASSED").put("reasonCode","FIXED_NOTICE_EXPECTATIONS_MATCHED")
                .put("expectedFileCount",2).put("discoveredFileCount",2).put("discoveryComplete",true)
                .put("allTextComplete",true).put("originalFilesRemoved",true).put("isPolicyQaPassed",false)
                .put("requestReservations",3).put("reservedBytes",280787);
        var files=result.putArray("files");
        for(int i=0;i<2;i++)files.addObject().put("status","PASSED").put("quality","COMPLETE_TEXT").put("roleAssessmentHash","a".repeat(64));
        return node;
    }
    @Test void fixedComparisonRequiresEveryFileAndNeverApprovesWholeCoverage() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(fixedReport()));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(null));
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(report()));
        for(String key:new String[]{"isPolicyQaPassed","isExpectationCoverageComplete","normalNotice"}) {
            var changed=fixedReport();changed.put(key,true);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
            changed.remove(key);assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        }
        var changed=fixedReport();changed.withObject("/result").withArray("files").remove(0);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        changed=fixedReport();((ObjectNode)changed.at("/result/files/0")).put("quality","PARTIAL_TEXT");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        changed=fixedReport();((ObjectNode)changed.at("/result/files/0")).remove("roleAssessmentHash");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        changed=fixedReport();changed.withObject("/result").put("caseId","TAEBAEK-176153");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
    }
    @Test void fixedComparisonReservesOnlyRemainingApprovedBudget() {
        for(String key:new String[]{"requestReservations","reservedBytes"}) {
            var changed=fixedReport();changed.withObject("/result").put(key,key.equals("requestReservations")?40:81508142);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
            changed.withObject("/result").put(key,0);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        }
        for(String key:new String[]{"allTextComplete","originalFilesRemoved","discoveryComplete"}) {
            var changed=fixedReport();changed.withObject("/result").put(key,false);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
        }
        var changed=fixedReport();changed.put("productionWriteCount",1);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectFixedReportComplete(changed));
    }
    private ObjectNode report() {
        var node = new ObjectMapper().createObjectNode();
        node.put("caseCode", "TAEBAEK-184816").put("scope", "OFFICIAL_THREE_STAGE_OBSERVATION_V1")
                .put("status", "OBSERVED_NOT_VALIDATED").put("productionWriteCount", 0)
                .put("isPolicyQaPassed", false).put("isExpectationApproved", false)
                .put("originalFilesRemoved", true).put("requiresFinalAdminVerification", true)
                .put("bodyStageComplete", true).put("maximumRequestReservations", 44)
                .put("maximumReservedBytes", 83886080L).put("requestReservationsIncludingBodyUpperBound", 5)
                .put("reservedBytesIncludingBodyUpperBound", 2377939);
        node.putArray("files").addObject();
        node.withArray("files").addObject();
        return node;
    }
    @Test void requiresOneActualTestWithoutFailureOrSkip() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectComplete(1, 1, 0, 0, 0, 0));
        for (long[] values : new long[][]{{0,0,0,0,0,0},{2,2,0,0,0,0},{1,0,1,0,0,0},
                {1,0,0,1,0,0},{1,0,0,0,1,0},{1,1,0,0,0,1}})
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectComplete(values[0], values[1], values[2], values[3], values[4], values[5]));
    }
    @Test void observationDoesNotPublishPolicyOrApproveExpectations() {
        assertTrue(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(report()));
        for (String key : new String[]{"isPolicyQaPassed", "isExpectationApproved"}) {
            var changed = report(); changed.put(key, true);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
            changed.remove(key);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        }
    }
    @Test void requiresBodyCleanupAndFinalAdministratorVerification() {
        for (String key : new String[]{"bodyStageComplete", "originalFilesRemoved", "requiresFinalAdminVerification"}) {
            var changed = report(); changed.put(key, false);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        }
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(null));
    }
    @Test void rejectsScopeDriftIncompleteAndOperatingWrites() {
        var changed = report(); changed.put("caseCode", "TAEBAEK-176153");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("scope", "OFFICIAL_WORKER_EPHEMERAL_DB_API_V1");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("status", "INCOMPLETE");
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("productionWriteCount", 1);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
    }
    @Test void rejectsMissingFilesAndBudgetChanges() {
        var changed = report(); changed.withArray("files").remove(0);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        for (String key : new String[]{"maximumRequestReservations", "maximumReservedBytes",
                "requestReservationsIncludingBodyUpperBound", "reservedBytesIncludingBodyUpperBound"}) {
            changed = report(); changed.put(key, -1);
            assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        }
        changed = report(); changed.put("reservedBytesIncludingBodyUpperBound", 83886081L);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
        changed = report(); changed.put("requestReservationsIncludingBodyUpperBound", 45);
        assertFalse(AnnouncementAttachmentBbsObservationProbe.selectReportComplete(changed));
    }
}

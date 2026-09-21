package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentBbsObservationProbeTest {
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

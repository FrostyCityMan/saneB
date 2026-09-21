package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentBbsObservationProbeTest {
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

package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttachmentCollectionOnlySummaryTest {
    @Test void successfulDownloadsDoNotClaimExtractionOrClassification() {
        var report=new LinkedHashMap<String,Object>();
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(report,List.of(Map.of("status","DOWNLOADED")),1);
        assertEquals(true,report.get("collectionStageComplete"));
        for(String key:List.of("isWholeTextAnalysisComplete","isExtractionVerified","isPolicyQaPassed","isExpectationApproved"))
            assertEquals(false,report.get(key));
        assertFalse(report.containsKey("decisionStatus"));
    }
    @Test void incompleteOrUnsupportedFilesNeverBecomeComplete() {
        for(String status:List.of("FAILED","NOT_RUN","UNSUPPORTED_NOT_DOWNLOADED","OBSERVED"))
            assertThrows(AssertionError.class,()->AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(
                    new LinkedHashMap<>(),List.of(Map.of("status",status)),1));
        assertThrows(AssertionError.class,()->AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(
                new LinkedHashMap<>(),List.of(),1));
    }
    @Test void confirmedEmptySetCanFinishWithoutClaimingFileDownload() {
        var report=new LinkedHashMap<String,Object>();
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(report,List.of(),0);
        assertEquals(true,report.get("collectionStageComplete"));
    }
}

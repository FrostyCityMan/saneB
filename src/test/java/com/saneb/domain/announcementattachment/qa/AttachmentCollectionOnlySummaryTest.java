package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttachmentCollectionOnlySummaryTest {
    @Test void batchSelectionIsExplicitSequentialAndDeduplicatedBeforeNetwork() {
        assertEquals(List.of("SUSEONG-52705","DALSEO-54290","DALSEO-53625","DALSEO-42293"),
                AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases("SUSEONG,DALSEO,SUSEONG").map(s->s.code()).toList());
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases("SUSEONG,"));
        assertThrows(IllegalArgumentException.class,()->AnnouncementAttachmentBbsOfficialObservationTest.selectBatchCases("SUSEONG,".repeat(9)));
    }
    @Test void successfulDownloadsDoNotClaimExtractionOrClassification() {
        var report=new LinkedHashMap<String,Object>();
        report.put("discoveryComplete",true);report.put("discoveryStatus","FOUND");
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(report,List.of(Map.of("status","DOWNLOADED")),1);
        assertEquals(true,report.get("collectionStageComplete"));
        for(String key:List.of("isWholeTextAnalysisComplete","isExtractionVerified","isPolicyQaPassed","isExpectationApproved"))
            assertEquals(false,report.get(key));
        assertFalse(report.containsKey("decisionStatus"));
    }
    @Test void incompleteOrUnsupportedFilesNeverBecomeComplete() {
        for(String status:List.of("FAILED","NOT_RUN","UNSUPPORTED_NOT_DOWNLOADED","OBSERVED")) {
            var report=new LinkedHashMap<String,Object>();
            report.put("discoveryComplete",true);report.put("discoveryStatus","FOUND");
            AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(report,
                    List.of(Map.of("status","DOWNLOADED"),Map.of("status",status)),2);
            assertEquals(false,report.get("collectionStageComplete"));
            assertEquals(1L,report.get("downloadedFileCount"));
            assertEquals("COLLECTION_ONLY_PARTIAL_NOT_APPROVED",report.get("status"));
        }
        var report=new LinkedHashMap<String,Object>();
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(report,List.of(),1);
        assertEquals(false,report.get("collectionStageComplete"));assertEquals(true,report.get("fileListChanged"));
    }
    @Test void confirmedEmptySetCanFinishWithoutClaimingFileDownload() {
        var report=new LinkedHashMap<String,Object>();
        report.put("discoveryComplete",true);report.put("discoveryStatus","NO_FILES");
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(report,List.of(),0);
        assertEquals(true,report.get("collectionStageComplete"));
    }
    @Test void missingOrPartialDiscoveryNeverClaimsCompleteEvenWithAllKnownFilesDownloaded() {
        var report=new LinkedHashMap<String,Object>();report.put("discoveryStatus","FAILED");report.put("discoveryComplete",false);
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionOnlySummary(report,List.of(Map.of("status","DOWNLOADED")),1);
        assertEquals(false,report.get("collectionStageComplete"));assertEquals(1L,report.get("downloadedFileCount"));
    }
}

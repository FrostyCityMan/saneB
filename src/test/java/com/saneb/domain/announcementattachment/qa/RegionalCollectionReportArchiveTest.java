package com.saneb.domain.announcementattachment.qa;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegionalCollectionReportArchiveTest {
    @TempDir Path directory;

    @Test void replayFileKeepsCaseIdentityAndPreviousReportBytes() throws Exception {
        var first=AnnouncementAttachmentBbsOfficialObservationTest.selectCollectionReportFile(directory,"JINJU-64420","");
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionReport(first,Map.of("caseCode","JINJU-64420","status","INCOMPLETE"));
        byte[] before=Files.readAllBytes(first);
        var retry=AnnouncementAttachmentBbsOfficialObservationTest.selectCollectionReportFile(directory,"JINJU-64420","RECHECK-20260930");
        assertThat(retry.getFileName().toString()).isEqualTo("JINJU-64420-RECHECK-20260930.json");
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionReport(retry,Map.of("caseCode","JINJU-64420","status","OBSERVED"));
        assertThat(new ObjectMapper().readTree(retry.toFile()).path("caseCode").asText()).isEqualTo("JINJU-64420");
        assertThat(Files.readAllBytes(first)).containsExactly(before);
    }

    @Test void duplicateIsRejectedBeforeRequestsAndAtomicWriteCannotReplaceEvidence() throws Exception {
        var path=AnnouncementAttachmentBbsOfficialObservationTest.selectCollectionReportFile(directory,"OSAN-50603","CHECK-1");
        AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionReport(path,Map.of("status","INCOMPLETE"));
        byte[] before=Files.readAllBytes(path);
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectCollectionReportFile(directory,"OSAN-50603","CHECK-1"))
                .hasMessage("COLLECTION_REPORT_ALREADY_EXISTS_USE_NEW_LABEL");
        assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.saveCollectionReport(path,Map.of("status","REPLACED")))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readAllBytes(path)).containsExactly(before);
    }

    @Test void rejectsPathsWhitespaceAndUnboundedIdentifiers() throws Exception {
        for(String label:List.of("../OTHER","a","/ABS","C:\\FILE","X/Y","X\\Y"," A","A ","A".repeat(65)))
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectCollectionReportFile(directory,"JINJU-64420",label))
                    .hasMessage("COLLECTION_REPORT_IDENTIFIER_INVALID");
        for(String code:List.of("../OTHER","","a","A".repeat(101)))
            assertThatThrownBy(()->AnnouncementAttachmentBbsOfficialObservationTest.selectCollectionReportFile(directory,code,"VALID"))
                    .hasMessage("COLLECTION_REPORT_IDENTIFIER_INVALID");
        try(var entries=Files.list(directory)){assertThat(entries.toList()).isEmpty();}
    }
}

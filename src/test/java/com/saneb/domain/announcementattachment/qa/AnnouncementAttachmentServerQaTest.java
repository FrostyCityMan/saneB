package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AnnouncementAttachmentServerQaTest {
    @Test void serverQaHasOnlyFourFixedPublicSamples() {
        var samples = AnnouncementAttachmentServerQa.selectSamples();
        assertEquals(4, samples.size());
        assertEquals(4, samples.stream().map(AnnouncementAttachmentServerQa.Sample::caseId).distinct().count());
        assertTrue(samples.stream().allMatch(s -> s.noticeId().matches("PBLN_[0-9]{15}")
                && s.fileId().matches("FILE_[0-9]{15}") && s.fileSn() >= 0));
    }

    @Test void rolesAreExplicitAndPosterIsNotPrimaryEvidence() {
        var samples = AnnouncementAttachmentServerQa.selectSamples();
        assertEquals(Set.of("PDF", "HWP", "HWPX"), samples.stream()
                .map(AnnouncementAttachmentServerQa.Sample::format).collect(java.util.stream.Collectors.toSet()));
        assertEquals("UNKNOWN", samples.stream().filter(s -> s.caseId().equals("PDF-SDM-2026")).findFirst().orElseThrow().role());
    }
}

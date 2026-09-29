package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AttachmentDraftSeedReuseTest {
    @Test void regionsShareOnlyImmutableSeedWithinOneJvm() throws Exception {
        var first=AnnouncementAttachmentRealFileQaTest.selectDraftQaContext();
        var second=AnnouncementAttachmentRealFileQaTest.selectDraftQaContext();
        assertSame(first,second);
        assertFalse(first.rules().rules().isEmpty());assertFalse(first.targets().isEmpty());
        assertThrows(UnsupportedOperationException.class,()->first.rules().rules().clear());
        assertThrows(UnsupportedOperationException.class,()->first.targets().clear());
    }
}

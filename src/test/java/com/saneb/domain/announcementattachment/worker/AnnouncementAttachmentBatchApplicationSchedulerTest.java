package com.saneb.domain.announcementattachment.worker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

class AnnouncementAttachmentBatchApplicationSchedulerTest {
    @Test void oneTickDispatchesOnlyOneApprovedItemAndRequiresExplicitWorkerFlag() {
        var service=mock(AnnouncementAttachmentBatchApplicationService.class);
        new AnnouncementAttachmentBatchApplicationScheduler(service).saveApplication();verify(service,times(1)).saveNextApplication();verifyNoMoreInteractions(service);
        var flag=AnnouncementAttachmentBatchApplicationScheduler.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(flag.prefix()).isEqualTo("saneb.announcement-attachment.worker");assertThat(flag.name()).containsExactly("enabled");
        assertThat(flag.havingValue()).isEqualTo("true");assertThat(flag.matchIfMissing()).isFalse();
    }
    @Test void failedTickDoesNotLoopOrTerminateScheduler() {
        var service=mock(AnnouncementAttachmentBatchApplicationService.class);when(service.saveNextApplication()).thenThrow(new IllegalStateException("fixture"));
        assertThatCode(()->new AnnouncementAttachmentBatchApplicationScheduler(service).saveApplication()).doesNotThrowAnyException();verify(service,times(1)).saveNextApplication();
    }
}

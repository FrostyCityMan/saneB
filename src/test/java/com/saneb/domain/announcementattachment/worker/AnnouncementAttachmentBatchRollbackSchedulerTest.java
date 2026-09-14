package com.saneb.domain.announcementattachment.worker;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
class AnnouncementAttachmentBatchRollbackSchedulerTest {
    @Test void rollbackHasSeparateOptInFromDownloadWorker(){var service=mock(AnnouncementAttachmentBatchRollbackService.class);new AnnouncementAttachmentBatchRollbackScheduler(service).saveRollback();verify(service).saveNextRollback();verifyNoMoreInteractions(service);var flag=AnnouncementAttachmentBatchRollbackScheduler.class.getAnnotation(ConditionalOnProperty.class);assertThat(flag.prefix()).isEqualTo("saneb.announcement-attachment.rollback");assertThat(flag.matchIfMissing()).isFalse();assertThat(flag.havingValue()).isEqualTo("true");}
    @Test void failureDoesNotLoopOrExposeUnderlyingException(){var service=mock(AnnouncementAttachmentBatchRollbackService.class);when(service.saveNextRollback()).thenThrow(new IllegalStateException("fixture"));assertThatCode(()->new AnnouncementAttachmentBatchRollbackScheduler(service).saveRollback()).doesNotThrowAnyException();verify(service,times(1)).saveNextRollback();}
}

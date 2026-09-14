package com.saneb.domain.announcementattachment.worker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaManagementService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

class AnnouncementAttachmentProviderQaSchedulerTest {
    @Test void schedulerRequiresExplicitProviderQaFlagAndClosesItsOwnWorker() throws Exception {
        var service=mock(AnnouncementAttachmentProviderQaManagementService.class);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(service.saveNextProviderQaRun()).thenAnswer(c->{entered.countDown();release.await(5,TimeUnit.SECONDS);return "IDLE";});
        var scheduler=new AnnouncementAttachmentProviderQaScheduler(service);
        try {scheduler.saveNextProviderQaRun();assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();scheduler.saveNextProviderQaRun();verify(service,times(1)).saveNextProviderQaRun();}
        finally {release.countDown();scheduler.close();}
        scheduler.saveNextProviderQaRun();verify(service,times(1)).saveNextProviderQaRun();
        var flag=AnnouncementAttachmentProviderQaScheduler.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(flag.prefix()).isEqualTo("saneb.announcement-attachment.provider-qa");assertThat(flag.matchIfMissing()).isFalse();assertThat(flag.havingValue()).isEqualTo("true");
    }
    @Test void exceptionDoesNotEscapeOrLoopInExecutor() throws Exception {
        var service=mock(AnnouncementAttachmentProviderQaManagementService.class);var entered=new CountDownLatch(1);
        when(service.saveNextProviderQaRun()).thenAnswer(c->{entered.countDown();throw new IllegalStateException("fixture-only");});
        var scheduler=new AnnouncementAttachmentProviderQaScheduler(service);
        try {assertThatCode(scheduler::saveNextProviderQaRun).doesNotThrowAnyException();assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();}
        finally {scheduler.close();}
        verify(service,times(1)).saveNextProviderQaRun();
    }
}

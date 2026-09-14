package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix="saneb.announcement-attachment.worker",name="enabled",havingValue="true",matchIfMissing=false)
public final class AnnouncementAttachmentWorkerScheduler {
    private static final Logger log=LoggerFactory.getLogger(AnnouncementAttachmentWorkerScheduler.class);
    private final AnnouncementAttachmentWorkerService worker;
    private final AtomicBoolean running=new AtomicBoolean();
    private final ExecutorService executor=Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon(true).name("saneb-attachment-worker").factory());
    public AnnouncementAttachmentWorkerScheduler(AnnouncementAttachmentWorkerService worker) { this.worker=worker; }
    @Scheduled(fixedDelayString="${saneb.announcement-attachment.worker.poll-delay-millis:5000}",
            initialDelayString="${saneb.announcement-attachment.worker.initial-delay-millis:15000}")
    public void saveNextAttachmentJob() {
        // 다른 수집 scheduler를 긴 파일 처리로 막지 않는다. 로컬 대기열은 한 작업으로 제한한다.
        if (!running.compareAndSet(false,true)) return;
        try { executor.submit(this::saveClaimedWork); }
        catch (java.util.concurrent.RejectedExecutionException exception) { running.set(false); }
    }
    private void saveClaimedWork() {
        try {
            var result=worker.saveNextAttachmentJob();
            if (!"IDLE".equals(result.statusCode())) log.info("첨부 작업 처리: jobId={}, status={}",result.jobId(),result.statusCode());
        } catch (RuntimeException exception) {
            // stack trace/외부 예외 원문은 기록하지 않는다.
            log.error("첨부 작업 처리에 실패했습니다. code=WORKER_PROCESSING_FAILED");
        } finally { running.set(false); }
    }
    @PreDestroy public void close() {
        executor.shutdownNow();
        try { executor.awaitTermination(35,TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}

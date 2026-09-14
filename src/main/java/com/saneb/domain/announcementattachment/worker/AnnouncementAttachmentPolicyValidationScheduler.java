package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyValidationService;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="saneb.announcement-attachment.policy-validation",name="enabled",havingValue="true",matchIfMissing=false)
public final class AnnouncementAttachmentPolicyValidationScheduler {
    private static final Logger log=LoggerFactory.getLogger(AnnouncementAttachmentPolicyValidationScheduler.class);
    private final AnnouncementAttachmentPolicyValidationService service;
    private final AtomicBoolean running=new AtomicBoolean();
    private final ExecutorService executor=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon(true).name("saneb-attachment-policy-qa").factory());
    public AnnouncementAttachmentPolicyValidationScheduler(AnnouncementAttachmentPolicyValidationService service) {this.service=service;}
    @Scheduled(fixedDelay=5000,initialDelay=15000)
    public void saveNextValidationRun() {
        if(!running.compareAndSet(false,true)) return;
        try {executor.submit(()->{
            try {String state=service.saveNextValidationRun();if(!"IDLE".equals(state)) log.info("첨부 정책 QA 처리 상태={}",state);}
            catch(RuntimeException exception) {log.error("첨부 정책 QA 처리 실패 code=POLICY_QA_PROCESSING_FAILED");}
            finally {running.set(false);}
        });}catch(RejectedExecutionException exception){running.set(false);}
    }
    @PreDestroy public void close() {
        executor.shutdownNow();
        try {executor.awaitTermination(35,TimeUnit.SECONDS);}catch(InterruptedException exception){Thread.currentThread().interrupt();}
    }
}

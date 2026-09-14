package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentProviderQaManagementService;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="saneb.announcement-attachment.provider-qa",name="enabled",havingValue="true",matchIfMissing=false)
public final class AnnouncementAttachmentProviderQaScheduler {
    private static final Logger log=LoggerFactory.getLogger(AnnouncementAttachmentProviderQaScheduler.class);
    private final AnnouncementAttachmentProviderQaManagementService service;
    private final AtomicBoolean running=new AtomicBoolean();
    private final ExecutorService executor=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon(true).name("saneb-attachment-provider-qa").factory());
    private volatile String previousState;
    public AnnouncementAttachmentProviderQaScheduler(AnnouncementAttachmentProviderQaManagementService service){this.service=service;}
    @Scheduled(fixedDelay=5000,initialDelay=15000)
    public void saveNextProviderQaRun() {
        if(!running.compareAndSet(false,true)) return;
        try {executor.submit(()->{
            try {
                String state=service.saveNextProviderQaRun();
                if(!"IDLE".equals(state) && !java.util.Objects.equals(previousState,state)) log.info("첨부 Provider QA 처리 상태={}",state);
                previousState=state;
            } catch(RuntimeException exception) {
                if(!"PROCESSING_FAILED".equals(previousState)) log.error("첨부 Provider QA 처리 실패 code=PROVIDER_QA_PROCESSING_FAILED");
                previousState="PROCESSING_FAILED";
            } finally {running.set(false);}
        });}catch(RejectedExecutionException exception){running.set(false);}
    }
    @PreDestroy public void close() {
        executor.shutdownNow();
        try {executor.awaitTermination(35,TimeUnit.SECONDS);}catch(InterruptedException exception){Thread.currentThread().interrupt();}
    }
}

package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** DB 집계만 실행한다. 파일 처리·HTTP·자동 적용·worker 시작 권한을 갖지 않는다. */
@Component
@ConditionalOnProperty(prefix="saneb.announcement-attachment.worker",name="enabled",havingValue="true",matchIfMissing=false)
public final class AnnouncementAttachmentBatchProgressScheduler {
    private static final Logger log=LoggerFactory.getLogger(AnnouncementAttachmentBatchProgressScheduler.class);
    private final AnnouncementAttachmentBatchService service;
    public AnnouncementAttachmentBatchProgressScheduler(AnnouncementAttachmentBatchService service) {this.service=service;}
    @Scheduled(fixedDelayString="${saneb.announcement-attachment.worker.batch-progress-delay-millis:15000}",initialDelay=15000)
    public void saveCollectionProgress() {
        try {service.saveCollectionProgress();}
        catch(RuntimeException exception) {log.error("첨부 배치 집계에 실패했습니다. code=BATCH_PROGRESS_FAILED");}
    }
}

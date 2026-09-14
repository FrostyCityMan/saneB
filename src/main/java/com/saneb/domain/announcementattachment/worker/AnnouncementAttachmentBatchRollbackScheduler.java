package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 다운로드 OFF와 별개인 DB 전용 복구다. 명시적 원복 승인 없이는 어떤 source도 변경하지 않는다. */
@Component
@ConditionalOnProperty(prefix="saneb.announcement-attachment.rollback",name="enabled",havingValue="true",matchIfMissing=false)
public final class AnnouncementAttachmentBatchRollbackScheduler {
    private static final Logger log=LoggerFactory.getLogger(AnnouncementAttachmentBatchRollbackScheduler.class);
    private final AnnouncementAttachmentBatchRollbackService service;
    public AnnouncementAttachmentBatchRollbackScheduler(AnnouncementAttachmentBatchRollbackService service){this.service=service;}
    @Scheduled(fixedDelayString="${saneb.announcement-attachment.worker.batch-rollback-delay-millis:1000}",initialDelay=15000)
    public void saveRollback(){try {service.saveNextRollback();}catch(RuntimeException ignored){log.error("첨부 배치 원복에 실패했습니다. code=BATCH_ROLLBACK_FAILED");}}
}

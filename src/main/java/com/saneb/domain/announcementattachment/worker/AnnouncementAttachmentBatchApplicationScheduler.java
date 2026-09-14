package com.saneb.domain.announcementattachment.worker;

import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchApplicationService;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 승인된 APPLYING 배치의 한 항목만 실행한다. 검수/전환/수집을 자동 시작하지 않는다. */
@Component
@ConditionalOnProperty(prefix="saneb.announcement-attachment.worker",name="enabled",havingValue="true",matchIfMissing=false)
public final class AnnouncementAttachmentBatchApplicationScheduler {
    private static final Logger log=LoggerFactory.getLogger(AnnouncementAttachmentBatchApplicationScheduler.class);
    private final AnnouncementAttachmentBatchApplicationService service;
    public AnnouncementAttachmentBatchApplicationScheduler(AnnouncementAttachmentBatchApplicationService service){this.service=service;}
    @Scheduled(fixedDelayString="${saneb.announcement-attachment.worker.batch-application-delay-millis:1000}",initialDelay=15000)
    public void saveApplication() {
        try {service.saveNextApplication();}catch(RuntimeException ignored){log.error("첨부 배치 적용에 실패했습니다. code=BATCH_APPLICATION_FAILED");}
    }
}

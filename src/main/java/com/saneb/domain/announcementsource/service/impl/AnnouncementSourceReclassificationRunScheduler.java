package com.saneb.domain.announcementsource.service.impl;

import com.saneb.domain.announcementsource.service.AnnouncementSourceReclassificationRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "saneb.announcement-source.reclassification-worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class AnnouncementSourceReclassificationRunScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementSourceReclassificationRunScheduler.class);
    private final AnnouncementSourceReclassificationRunService service;

    public AnnouncementSourceReclassificationRunScheduler(AnnouncementSourceReclassificationRunService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${saneb.announcement-source.reclassification-worker.poll-delay-millis:2000}",
            initialDelayString = "${saneb.announcement-source.reclassification-worker.initial-delay-millis:15000}"
    )
    public void insertNextRunBatch() {
        try {
            service.insertNextRunBatch();
        } catch (RuntimeException exception) {
            log.error("공고 재분류 배치 worker 실행에 실패했습니다.", exception);
        }
    }
}

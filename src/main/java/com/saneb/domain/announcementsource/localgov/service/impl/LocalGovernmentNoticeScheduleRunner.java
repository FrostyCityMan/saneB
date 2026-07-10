/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeScheduleRunner.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.service.impl;

import com.saneb.domain.announcementsource.localgov.service.LocalGovernmentNoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "saneb.announcement-source.local-government.schedule",
        name = "enabled",
        havingValue = "true"
)
public class LocalGovernmentNoticeScheduleRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalGovernmentNoticeScheduleRunner.class);

    private final LocalGovernmentNoticeService localGovernmentNoticeService;

    /**
     * 승인 스케줄 실행기를 생성합니다.
     *
     * @param localGovernmentNoticeService 지자체 공고 URL 관리 서비스
     */
    public LocalGovernmentNoticeScheduleRunner(LocalGovernmentNoticeService localGovernmentNoticeService) {
        this.localGovernmentNoticeService = localGovernmentNoticeService;
    }

    /**
     * DB에 승인된 실행 예정 스케줄을 주기적으로 처리합니다.
     */
    @Scheduled(
            fixedDelayString = "$" + "{saneb.announcement-source.local-government.schedule.poll-delay-millis:60000}",
            initialDelayString = "$" + "{saneb.announcement-source.local-government.schedule.initial-delay-millis:30000}"
    )
    public void executeDueSchedules() {
        try {
            localGovernmentNoticeService.executeDueSchedules();
        } catch (RuntimeException exception) {
            log.error("Approved local-government notice schedule execution failed.", exception);
        }
    }
}

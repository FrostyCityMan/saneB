/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceCollectionScheduler.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "saneb.announcement-source.collection.batch", name = "enabled", havingValue = "true")
public class AnnouncementSourceCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementSourceCollectionScheduler.class);

    private final AnnouncementSourceService announcementSourceService;
    private final List<String> providerCodes;
    private final int maxCount;

    /**
     * 객체를 생성합니다.
     *
     * @param announcementSourceService 입력 값
     *
     * @param providerCodes 입력 값
     *
     * @param maxCount 입력 값
     */
    public AnnouncementSourceCollectionScheduler(
            AnnouncementSourceService announcementSourceService,
            @Value("$" + "{saneb.announcement-source.collection.batch.provider-codes:BIZINFO}") String providerCodes,
            @Value("$" + "{saneb.announcement-source.collection.batch.max-count:100}") int maxCount
    ) {
        this.announcementSourceService = announcementSourceService;
        this.providerCodes = Arrays.stream(providerCodes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        this.maxCount = Math.max(1, maxCount);
    }

    /**
     * 배치 수집 승인 요청을 생성합니다.
     */
    @Scheduled(cron = "$" + "{saneb.announcement-source.collection.batch.cron:0 0 8 * * *}")
    public void insertScheduledCollectionRequests() {
        for (String providerCode : providerCodes) {
            try {
                announcementSourceService.insertBatchCollectionRequest(providerCode, maxCount);
            } catch (RuntimeException exception) {
                log.error("Failed to insert announcement source batch request. providerCode={}", providerCode, exception);
            }
        }
    }
}

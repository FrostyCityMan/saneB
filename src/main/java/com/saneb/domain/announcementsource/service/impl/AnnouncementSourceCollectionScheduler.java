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

import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final Map<String, AnnouncementSourceProviderClient> providerClients;
    private final List<String> providerCodes;
    private final int maxCount;

    /**
     * 객체를 생성합니다.
     *
     * @param announcementSourceService 입력 값
     * @param providerClients 외부 공고 제공자
     * @param providerCodes 입력 값
     * @param maxCount 입력 값
     */
    public AnnouncementSourceCollectionScheduler(
            AnnouncementSourceService announcementSourceService,
            List<AnnouncementSourceProviderClient> providerClients,
            @Value("$" + "{saneb.announcement-source.collection.batch.provider-codes:BIZINFO}") String providerCodes,
            @Value("$" + "{saneb.announcement-source.collection.batch.max-count:100}") int maxCount
    ) {
        this.announcementSourceService = announcementSourceService;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AnnouncementSourceProviderClient::selectProviderCode,
                        Function.identity()
                ));
        this.providerCodes = Arrays.stream(providerCodes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        this.maxCount = Math.max(1, maxCount);
    }

    /**
     * 배치 수집 승인 요청을 생성합니다.
     */
    @Scheduled(
            cron = "$" + "{saneb.announcement-source.collection.batch.cron:0 0 8 * * *}",
            zone = "Asia/Seoul"
    )
    public void insertScheduledCollectionRequests() {
        for (String providerCode : providerCodes) {
            try {
                AnnouncementSourceProviderClient providerClient = providerClients.get(providerCode);
                if (providerClient == null || !providerClient.isConfigured()) {
                    log.warn(
                            "Skipping announcement source batch request because provider configuration is incomplete. providerCode={}",
                            providerCode
                    );
                    continue;
                }
                announcementSourceService.insertBatchCollectionRequest(providerCode, maxCount);
            } catch (RuntimeException exception) {
                log.error("Failed to insert announcement source batch request. providerCode={}", providerCode, exception);
            }
        }
    }
}

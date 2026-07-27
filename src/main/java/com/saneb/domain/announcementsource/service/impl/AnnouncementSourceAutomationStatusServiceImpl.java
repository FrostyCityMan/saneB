/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceAutomationStatusServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceAutomationStatusResponse;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import com.saneb.domain.announcementsource.service.AnnouncementSourceAutomationStatusService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementSourceAutomationStatusServiceImpl implements AnnouncementSourceAutomationStatusService {

    private final Map<String, AnnouncementSourceProviderClient> providerClients;
    private final boolean localGovernmentScheduleEnabled;
    private final boolean providerBatchEnabled;

    /**
     * 자동수집 설정 상태 조회 서비스를 생성합니다.
     *
     * @param providerClients 외부 공고 제공자
     * @param localGovernmentScheduleEnabled 지자체 승인 일정 실행 여부
     * @param providerBatchEnabled API 제공자 승인요청 자동 생성 여부
     */
    public AnnouncementSourceAutomationStatusServiceImpl(
            List<AnnouncementSourceProviderClient> providerClients,
            @Value("$" + "{saneb.announcement-source.local-government.schedule.enabled:false}")
            boolean localGovernmentScheduleEnabled,
            @Value("$" + "{saneb.announcement-source.collection.batch.enabled:false}")
            boolean providerBatchEnabled
    ) {
        this.providerClients = providerClients.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AnnouncementSourceProviderClient::selectProviderCode,
                        Function.identity()
                ));
        this.localGovernmentScheduleEnabled = localGovernmentScheduleEnabled;
        this.providerBatchEnabled = providerBatchEnabled;
    }

    /**
     * 외부 공고 자동수집과 제공자 설정 준비 상태를 조회합니다.
     *
     * @return 자동수집 준비 상태
     */
    @Override
    public AnnouncementSourceAutomationStatusResponse selectStatus() {
        return new AnnouncementSourceAutomationStatusResponse(
                localGovernmentScheduleEnabled,
                providerBatchEnabled,
                isProviderConfigured("BIZINFO"),
                isProviderConfigured("GOV24_PUBLIC_SERVICE")
        );
    }

    /**
     * 지정한 외부 제공자의 호출 설정이 준비됐는지 확인합니다.
     *
     * @param providerCode 외부 제공자 코드
     * @return 호출 가능하면 true
     */
    private boolean isProviderConfigured(String providerCode) {
        AnnouncementSourceProviderClient providerClient = providerClients.get(providerCode);
        return providerClient != null && providerClient.isConfigured();
    }
}

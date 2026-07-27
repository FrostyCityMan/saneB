/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceCollectionSchedulerTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import com.saneb.domain.announcementsource.service.AnnouncementSourceService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementSourceCollectionSchedulerTest {

    /**
     * 설정이 완료된 외부 제공자에 대해서만 배치 승인요청을 생성하는지 확인합니다.
     */
    @Test
    void insertScheduledCollectionRequestsSkipsUnconfiguredProvider() {
        AnnouncementSourceService announcementSourceService = mock(AnnouncementSourceService.class);
        AnnouncementSourceProviderClient bizInfo = provider("BIZINFO", true);
        AnnouncementSourceProviderClient gov24 = provider("GOV24_PUBLIC_SERVICE", false);
        AnnouncementSourceCollectionScheduler scheduler = new AnnouncementSourceCollectionScheduler(
                announcementSourceService,
                List.of(bizInfo, gov24),
                "BIZINFO,GOV24_PUBLIC_SERVICE",
                100
        );

        scheduler.insertScheduledCollectionRequests();

        verify(announcementSourceService).insertBatchCollectionRequest("BIZINFO", 100);
        verify(announcementSourceService, never()).insertBatchCollectionRequest("GOV24_PUBLIC_SERVICE", 100);
    }

    /**
     * 테스트용 외부 공고 제공자를 생성합니다.
     *
     * @param providerCode 제공자 코드
     * @param configured 설정 완료 여부
     * @return 테스트 제공자
     */
    private AnnouncementSourceProviderClient provider(String providerCode, boolean configured) {
        AnnouncementSourceProviderClient provider = mock(AnnouncementSourceProviderClient.class);
        when(provider.selectProviderCode()).thenReturn(providerCode);
        when(provider.isConfigured()).thenReturn(configured);
        return provider;
    }
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceAutomationStatusServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceAutomationStatusResponse;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementSourceAutomationStatusServiceImplTest {

    /**
     * 실행 설정과 외부 제공자 준비 상태를 비밀값 없이 반환하는지 확인합니다.
     */
    @Test
    void selectStatusReturnsAutomationReadiness() {
        AnnouncementSourceProviderClient bizInfo = provider("BIZINFO", true);
        AnnouncementSourceProviderClient gov24 = provider("GOV24_PUBLIC_SERVICE", false);
        AnnouncementSourceAutomationStatusServiceImpl service =
                new AnnouncementSourceAutomationStatusServiceImpl(List.of(bizInfo, gov24), true, true);

        AnnouncementSourceAutomationStatusResponse result = service.selectStatus();

        assertThat(result.localGovernmentScheduleEnabled()).isTrue();
        assertThat(result.providerBatchEnabled()).isTrue();
        assertThat(result.bizInfoConfigured()).isTrue();
        assertThat(result.gov24Configured()).isFalse();
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

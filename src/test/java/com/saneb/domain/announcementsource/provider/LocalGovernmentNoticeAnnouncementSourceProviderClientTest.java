/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeAnnouncementSourceProviderClientTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcementsource.dao.LocalGovernmentNoticeDao;
import com.saneb.domain.announcementsource.localgov.collector.LocalGovernmentNoticeCollector;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceSemanticFilter;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeCollectionResultCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceCollectionStatusCommand;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LocalGovernmentNoticeAnnouncementSourceProviderClientTest {

    /**
     * 출처별 내부 예외가 실행 전체에서 사라지지 않고 안전한 진단 결과로 저장되는지 검증합니다.
     */
    @Test
    void selectSourceBatchStoresProcessingFailureWhenSourceHandlingThrows() {
        LocalGovernmentNoticeDao dao = mock(LocalGovernmentNoticeDao.class);
        LocalGovernmentNoticeCollector collector = mock(LocalGovernmentNoticeCollector.class);
        AnnouncementSourceSemanticFilter semanticFilter = mock(AnnouncementSourceSemanticFilter.class);
        LocalGovernmentNoticeAnnouncementSourceProviderClient client =
                new LocalGovernmentNoticeAnnouncementSourceProviderClient(dao, collector, semanticFilter);
        UUID sourceId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        when(dao.selectSemanticKeywordRuleList()).thenReturn(List.of());
        when(dao.selectEnabledSourceList(sourceId, 1)).thenReturn(List.of(source(sourceId)));
        when(dao.selectParserProfileDetails("SPRING_BBS"))
                .thenThrow(new IllegalStateException("test failure"));

        AnnouncementSourceProviderBatch batch = client.selectSourceBatch(request(sourceId), runId);

        ArgumentCaptor<LocalGovernmentNoticeCollectionResultCommand> resultCaptor =
                ArgumentCaptor.forClass(LocalGovernmentNoticeCollectionResultCommand.class);
        ArgumentCaptor<LocalGovernmentNoticeSourceCollectionStatusCommand> statusCaptor =
                ArgumentCaptor.forClass(LocalGovernmentNoticeSourceCollectionStatusCommand.class);
        verify(dao).insertCollectionResult(resultCaptor.capture());
        verify(dao).updateSourceCollectionStatus(statusCaptor.capture());

        assertThat(batch.items()).isEmpty();
        assertThat(batch.failedCount()).isEqualTo(1);
        assertThat(batch.errorMessage()).contains("1개 기관 URL");
        assertThat(resultCaptor.getValue().errorCode()).isEqualTo("PROCESSING_FAILED");
        assertThat(resultCaptor.getValue().errorMessage()).doesNotContain("test failure");
        assertThat(statusCaptor.getValue().errorCode()).isEqualTo("PROCESSING_FAILED");
        assertThat(statusCaptor.getValue().success()).isFalse();
        verify(collector, never()).collect(any(), any());
    }

    /**
     * 테스트용 지자체 출처를 생성합니다.
     *
     * @param sourceId 출처 식별자
     * @return 의미 검증이 완료된 활성 출처
     */
    private LocalGovernmentNoticeSourceRow source(UUID sourceId) {
        return new LocalGovernmentNoticeSourceRow(
                sourceId, "LGS-TEST", "11", "서울특별시", "110", "테스트구",
                "BASIC_LOCAL_GOVERNMENT", "테스트구청", "https://example.go.kr",
                "https://example.go.kr/notice", null, "official_news_url", "DEFAULT", "GET", null,
                "SPRING_BBS", "일반 표형 게시판", null, "HIGH", "VERIFIED",
                "LEGAL_NOTICE", "COLLECT_ALL", true, null, null, "공식 게시판 확인",
                true, "READY", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
    }

    /**
     * 테스트용 단일 출처 수집 요청을 생성합니다.
     *
     * @param sourceId 출처 식별자
     * @return 승인된 단일 출처 요청
     */
    private AnnouncementSourceCollectionRequestRow request(UUID sourceId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AnnouncementSourceCollectionRequestRow(
                UUID.randomUUID(), "ASR-TEST", "LOCAL_GOV_NOTICE", "MANUAL", "APPROVED",
                UUID.randomUUID(), now, "TEST", null, null, null, null, null, 1,
                "테스트", sourceId, null, UUID.randomUUID(), now, "테스트 승인", now, now
        );
    }
}

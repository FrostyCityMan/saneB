/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceToAnnouncementRequest;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderAttachment;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceHighlightService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAttachmentCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunItemCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AnnouncementSourceServiceImplTest {

    private static final UUID REQUEST_ID = UUID.fromString("93000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_ID = UUID.fromString("93000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-20T10:00:00+09:00");

    @Mock
    private AnnouncementSourceDao announcementSourceDao;

    @Mock
    private AnnouncementDao announcementDao;

    @Mock
    private AnnouncementSourceHighlightService highlightService;

    @Mock
    private AnnouncementSourceProviderClient providerClient;

    private AnnouncementSourceServiceImpl service;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(providerClient.selectProviderCode()).thenReturn("BIZINFO");
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                announcementDao,
                highlightService,
                List.of(providerClient)
        );
    }

    /**
     * 승인 전 실행 차단을 확인합니다.
     */
    @Test
    void insertCollectionRunRejectsPendingRequest() {
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVAL_PENDING"));

        assertThatThrownBy(() -> service.insertCollectionRun(REQUEST_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("승인된 수집 요청만 실행");

        verify(providerClient, never()).selectSourceItemList(any());
        verify(announcementSourceDao, never()).insertCollectionRun(any());
    }

    /**
     * 종료 공고 제외 처리를 확인합니다.
     */
    @Test
    void insertCollectionRunSkipsEndedAnnouncement() {
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any()))
                .thenReturn(List.of(providerItem("BIZ-OLD", LocalDate.now().minusDays(1))));
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(invocation.getArgument(0), "COMPLETED", 1, 0, 1, 0, 0));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.skippedEndedCount()).isEqualTo(1);
        verify(announcementSourceDao, never()).insertSourceSnapshot(any());
        verify(announcementSourceDao).insertCollectionRunItem(any());
    }

    /**
     * 중복 공고 처리를 확인합니다.
     */
    @Test
    void insertCollectionRunMarksDuplicateAnnouncement() {
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any()))
                .thenReturn(List.of(providerItem("BIZ-DUP", LocalDate.now().plusDays(10))));
        when(announcementSourceDao.selectSourceByProviderNoticeId("BIZINFO", "BIZ-DUP"))
                .thenReturn(sourceRow());
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(invocation.getArgument(0), "COMPLETED", 1, 0, 0, 1, 0));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.duplicateCount()).isEqualTo(1);
        verify(announcementSourceDao, never()).insertSourceSnapshot(any());
        verify(announcementSourceDao).insertCollectionRunItem(any());
    }

    /**
     * 신규 원문, 첨부, 하이라이트 저장을 확인합니다.
     */
    @Test
    void insertCollectionRunStoresNewSourceAndReferenceHighlights() {
        AnnouncementSourceProviderItem item = providerItem("BIZ-NEW", LocalDate.now().plusDays(10));
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any())).thenReturn(List.of(item));
        when(announcementSourceDao.selectSourceByProviderNoticeId("BIZINFO", "BIZ-NEW")).thenReturn(null);
        when(announcementSourceDao.selectSourceByUrl("BIZINFO", item.sourceUrl())).thenReturn(null);
        when(announcementSourceDao.selectSourceByRawHash("BIZINFO", item.rawHash())).thenReturn(null);
        when(highlightService.selectHighlightList(any(), any(), any(), any()))
                .thenReturn(List.of(new AnnouncementSourceHighlightCommand(
                        UUID.randomUUID(),
                        SOURCE_ID,
                        "TARGET",
                        "지원대상",
                        0,
                        4,
                        1,
                        "RULE_KEYWORD"
                )));
        when(announcementSourceDao.selectActiveAnnouncementDuplicateCandidateList(any()))
                .thenReturn(List.of(duplicateCandidateRow()));
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(invocation.getArgument(0), "COMPLETED", 1, 1, 0, 0, 0));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.collectedCount()).isEqualTo(1);
        ArgumentCaptor<AnnouncementSourceSnapshotCommand> snapshotCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceSnapshotCommand.class);
        ArgumentCaptor<AnnouncementSourceCollectionRunItemCommand> runItemCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceCollectionRunItemCommand.class);
        ArgumentCaptor<AnnouncementSourceDuplicateCandidateCommand> duplicateCandidateCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceDuplicateCandidateCommand.class);
        verify(announcementSourceDao).insertSourceSnapshot(snapshotCaptor.capture());
        verify(announcementSourceDao).insertSourceAttachment(any(AnnouncementSourceAttachmentCommand.class));
        verify(announcementSourceDao).insertSourceHighlight(any(AnnouncementSourceHighlightCommand.class));
        verify(announcementSourceDao).insertDuplicateCandidate(duplicateCandidateCaptor.capture());
        verify(announcementSourceDao).insertCollectionRunItem(runItemCaptor.capture());
        assertThat(snapshotCaptor.getValue().reviewStatusCode()).isEqualTo("REVIEW_PENDING");
        assertThat(duplicateCandidateCaptor.getValue().matchTypeCode()).isEqualTo("EXACT_DUPLICATE");
        assertThat(runItemCaptor.getValue().itemStatusCode()).isEqualTo("COLLECTED");
    }

    /**
     * 보류 중복/유사 후보가 있으면 신규 DRAFT 생성을 차단합니다.
     */
    @Test
    void insertOperationalAnnouncementRejectsPendingDuplicateCandidates() {
        when(announcementSourceDao.selectSourceDetails(SOURCE_ID)).thenReturn(sourceRow());
        when(announcementSourceDao.selectPendingDuplicateCandidateCount(SOURCE_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.insertOperationalAnnouncement(
                authentication(),
                SOURCE_ID,
                new AnnouncementSourceToAnnouncementRequest("BUSINESS", "VAT_TAX_BASE_ONLY")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("중복 또는 유사 공고 후보");

        verify(announcementDao, never()).insertAnnouncement(any());
    }

    /**
     * 수집 요청 row를 생성합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRequestRow collectionRequest(String statusCode) {
        return new AnnouncementSourceCollectionRequestRow(
                REQUEST_ID,
                "ASR-000001",
                "BIZINFO",
                "MANUAL",
                statusCode,
                UUID.randomUUID(),
                NOW,
                "ADMIN_BUTTON",
                "소상공인",
                null,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                100,
                "요청",
                null,
                null,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    /**
     * 수집 실행 row를 생성합니다.
     *
     * @param runId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param totalCount 입력 값
     *
     * @param collectedCount 입력 값
     *
     * @param skippedEndedCount 입력 값
     *
     * @param duplicateCount 입력 값
     *
     * @param failedCount 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRunRow collectionRun(
            UUID runId,
            String statusCode,
            int totalCount,
            int collectedCount,
            int skippedEndedCount,
            int duplicateCount,
            int failedCount
    ) {
        return new AnnouncementSourceCollectionRunRow(
                runId,
                "ASRUN-000001",
                REQUEST_ID,
                "ASR-000001",
                "BIZINFO",
                "MANUAL",
                statusCode,
                NOW,
                NOW,
                totalCount,
                collectedCount,
                skippedEndedCount,
                duplicateCount,
                failedCount,
                null,
                NOW
        );
    }

    /**
     * provider item을 생성합니다.
     *
     * @param providerNoticeId 입력 값
     *
     * @param applicationEndDate 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceProviderItem providerItem(String providerNoticeId, LocalDate applicationEndDate) {
        return new AnnouncementSourceProviderItem(
                "BIZINFO",
                providerNoticeId,
                "소상공인 지원사업",
                "중소벤처기업부",
                LocalDate.now(),
                applicationEndDate,
                NOW,
                NOW,
                "https://example.com/" + providerNoticeId,
                "지원대상 소상공인\n신청기간 안내",
                "문의처 1357",
                "온라인 신청",
                "COMPLETE",
                "{}",
                "{\"title\":\"소상공인 지원사업\"}",
                "hash-" + providerNoticeId,
                List.of(new AnnouncementSourceProviderAttachment(
                        "공고문.pdf",
                        "https://example.com/file.pdf",
                        "PDF"
                )),
                null
        );
    }

    /**
     * 수집 원문 row를 생성합니다.
     *
     * @return 처리 결과
     */
    private AnnouncementSourceSnapshotRow sourceRow() {
        return new AnnouncementSourceSnapshotRow(
                SOURCE_ID,
                "SRC-000001",
                "BIZINFO",
                "BIZ-DUP",
                "소상공인 지원사업",
                "중소벤처기업부",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                NOW,
                NOW,
                "https://example.com/BIZ-DUP",
                "원문",
                "문의",
                "온라인",
                "COMPLETE",
                "{}",
                "hash-BIZ-DUP",
                "REVIEW_PENDING",
                NOW,
                NOW,
                NOW
        );
    }

    /**
     * 중복 후보 row를 생성합니다.
     *
     * @return 처리 결과
     */
    private AnnouncementSourceDuplicateCandidateRow duplicateCandidateRow() {
        return new AnnouncementSourceDuplicateCandidateRow(
                UUID.randomUUID(),
                SOURCE_ID,
                UUID.randomUUID(),
                "ANN-000001",
                "소상공인 지원사업",
                "중소벤처기업부",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "NORMAL",
                "APPROVED",
                "SRC-000000",
                "BIZ-NEW",
                "https://example.com/BIZ-NEW",
                "EXACT_DUPLICATE",
                true,
                true,
                true,
                true,
                true,
                "사업명 일치, 주관기관 일치, 공고번호 일치, 신청기간 일치, 원문 URL 일치",
                "PENDING",
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    /**
     * 인증 정보를 생성합니다.
     *
     * @return 처리 결과
     */
    private Authentication authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.randomUUID(),
                        "operator",
                        "password-hash",
                        "운영자",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("OPERATOR")
        );
        return new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities());
    }
}

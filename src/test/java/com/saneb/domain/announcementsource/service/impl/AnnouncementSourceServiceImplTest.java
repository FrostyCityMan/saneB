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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dao.LocalGovernmentNoticeDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceCollectionRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceDuplicateDecisionRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceToAnnouncementRequest;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderAttachment;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderBatch;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderClient;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.provider.content.ProviderContentClient;
import com.saneb.domain.announcementsource.provider.content.ProviderContentRequest;
import com.saneb.domain.announcementsource.provider.content.ProviderContentResult;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationCoordinator;
import com.saneb.domain.announcementsource.service.AnnouncementSourceHighlightService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceAttachmentCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunItemCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRunRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceCollectionRequestRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceDuplicateCandidateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceHighlightCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceLinkedAnnouncementRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRefreshCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.net.URI;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AnnouncementSourceServiceImplTest {

    private static final UUID REQUEST_ID = UUID.fromString("93000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_ID = UUID.fromString("93000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-20T10:00:00+09:00");

    @Mock
    private AnnouncementSourceDao announcementSourceDao;

    @Mock
    private LocalGovernmentNoticeDao localGovernmentNoticeDao;

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
                localGovernmentNoticeDao,
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
        verify(announcementSourceDao, never()).updateSourceSnapshotContent(any());
        verify(announcementSourceDao).insertCollectionRunItem(any());
    }

    @Test
    void insertCollectionRunAppendsChangedExistingSourceContent() {
        AnnouncementSourceProviderItem changedItem = providerItem(
                "BIZ-DUP",
                LocalDate.now().plusDays(10)
        );
        AnnouncementSourceClassificationCoordinator coordinator =
                mock(AnnouncementSourceClassificationCoordinator.class);
        UUID releaseId = UUID.fromString("93000000-0000-0000-0000-000000000030");
        AnnouncementSourceClassificationCoordinator.RunContext runContext =
                new AnnouncementSourceClassificationCoordinator.RunContext(true, releaseId, null, null);
        AnnouncementSourceClassificationCoordinator.PreparedClassification preparedClassification =
                new AnnouncementSourceClassificationCoordinator.PreparedClassification(
                        true,
                        releaseId,
                        changedItem,
                        null
                );
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                localGovernmentNoticeDao,
                announcementDao,
                highlightService,
                List.of(providerClient),
                List.of(),
                coordinator,
                null
        );
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any())).thenReturn(List.of(changedItem));
        when(coordinator.selectRunContext(any(), eq("BIZINFO"))).thenReturn(runContext);
        when(coordinator.selectClassification(eq(runContext), eq(changedItem), any(), any()))
                .thenReturn(preparedClassification);
        when(announcementSourceDao.selectSourceByProviderNoticeId("BIZINFO", "BIZ-DUP"))
                .thenReturn(sourceRow("old-raw-hash", 7));
        when(coordinator.selectContentVersionAppendRequired(SOURCE_ID, preparedClassification))
                .thenReturn(true);
        when(announcementSourceDao.updateSourceSnapshotContent(any())).thenReturn(1);
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(
                        invocation.getArgument(0), "COMPLETED", 1, 1, 0, 0, 0
                ));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.collectedCount()).isEqualTo(1);
        ArgumentCaptor<AnnouncementSourceSnapshotRefreshCommand> refreshCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceSnapshotRefreshCommand.class);
        verify(announcementSourceDao).updateSourceSnapshotContent(refreshCaptor.capture());
        assertThat(refreshCaptor.getValue().sourceId()).isEqualTo(SOURCE_ID);
        assertThat(refreshCaptor.getValue().expectedRawHash()).isEqualTo("old-raw-hash");
        assertThat(refreshCaptor.getValue().rawHash()).isEqualTo(changedItem.rawHash());
        assertThat(refreshCaptor.getValue().expectedVersion()).isEqualTo(7);
        verify(coordinator).saveChangedClassification(
                eq(SOURCE_ID),
                any(),
                eq(preparedClassification),
                eq("REVIEW_PENDING"),
                eq(7)
        );
        verify(announcementSourceDao, never()).insertSourceSnapshot(any());
        ArgumentCaptor<AnnouncementSourceCollectionRunItemCommand> runItemCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceCollectionRunItemCommand.class);
        verify(announcementSourceDao).insertCollectionRunItem(runItemCaptor.capture());
        assertThat(runItemCaptor.getValue().sourceId()).isEqualTo(SOURCE_ID);
        assertThat(runItemCaptor.getValue().itemStatusCode()).isEqualTo("COLLECTED");
    }

    @Test
    void insertCollectionRunAppendsWhenDetailBodyChangesWithSameProviderRawHash() {
        AnnouncementSourceProviderItem changedItem = providerItem(
                "BIZ-DUP",
                LocalDate.now().plusDays(10)
        ).withBodyText("변경된 상세 본문");
        AnnouncementSourceClassificationCoordinator coordinator =
                mock(AnnouncementSourceClassificationCoordinator.class);
        UUID releaseId = UUID.fromString("93000000-0000-0000-0000-000000000031");
        AnnouncementSourceClassificationCoordinator.RunContext runContext =
                new AnnouncementSourceClassificationCoordinator.RunContext(true, releaseId, null, null);
        AnnouncementSourceClassificationCoordinator.PreparedClassification preparedClassification =
                new AnnouncementSourceClassificationCoordinator.PreparedClassification(
                        true, releaseId, changedItem, null
                );
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                localGovernmentNoticeDao,
                announcementDao,
                highlightService,
                List.of(providerClient),
                List.of(),
                coordinator,
                null
        );
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any())).thenReturn(List.of(changedItem));
        when(coordinator.selectRunContext(any(), eq("BIZINFO"))).thenReturn(runContext);
        when(coordinator.selectClassification(eq(runContext), eq(changedItem), any(), any()))
                .thenReturn(preparedClassification);
        when(announcementSourceDao.selectSourceByProviderNoticeId("BIZINFO", "BIZ-DUP"))
                .thenReturn(sourceRow(changedItem.rawHash(), 8));
        when(coordinator.selectContentVersionAppendRequired(SOURCE_ID, preparedClassification))
                .thenReturn(true);
        when(announcementSourceDao.updateSourceSnapshotContent(any())).thenReturn(1);
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(
                        invocation.getArgument(0), "COMPLETED", 1, 1, 0, 0, 0
                ));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.collectedCount()).isEqualTo(1);
        ArgumentCaptor<AnnouncementSourceSnapshotRefreshCommand> refreshCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceSnapshotRefreshCommand.class);
        verify(announcementSourceDao).updateSourceSnapshotContent(refreshCaptor.capture());
        assertThat(refreshCaptor.getValue().expectedRawHash()).isEqualTo(changedItem.rawHash());
        assertThat(refreshCaptor.getValue().rawHash()).isEqualTo(changedItem.rawHash());
        assertThat(refreshCaptor.getValue().bodyText()).isEqualTo("변경된 상세 본문");
        assertThat(refreshCaptor.getValue().canonicalSourceUrl())
                .isEqualTo("https://example.com/BIZ-DUP");
        verify(coordinator).saveChangedClassification(
                eq(SOURCE_ID), any(), eq(preparedClassification), eq("REVIEW_PENDING"), eq(8)
        );
        verify(announcementSourceDao, never()).insertSourceSnapshot(any());
    }

    @Test
    void insertCollectionRunKeepsDuplicateWhenCanonicalContentIsUnchanged() {
        AnnouncementSourceProviderItem item = providerItem(
                "BIZ-DUP",
                LocalDate.now().plusDays(10)
        );
        AnnouncementSourceClassificationCoordinator coordinator =
                mock(AnnouncementSourceClassificationCoordinator.class);
        UUID releaseId = UUID.fromString("93000000-0000-0000-0000-000000000032");
        AnnouncementSourceClassificationCoordinator.RunContext runContext =
                new AnnouncementSourceClassificationCoordinator.RunContext(true, releaseId, null, null);
        AnnouncementSourceClassificationCoordinator.PreparedClassification preparedClassification =
                new AnnouncementSourceClassificationCoordinator.PreparedClassification(
                        true, releaseId, item, null
                );
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                localGovernmentNoticeDao,
                announcementDao,
                highlightService,
                List.of(providerClient),
                List.of(),
                coordinator,
                null
        );
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any())).thenReturn(List.of(item));
        when(coordinator.selectRunContext(any(), eq("BIZINFO"))).thenReturn(runContext);
        when(coordinator.selectClassification(eq(runContext), eq(item), any(), any()))
                .thenReturn(preparedClassification);
        when(announcementSourceDao.selectSourceByProviderNoticeId("BIZINFO", "BIZ-DUP"))
                .thenReturn(sourceRow(item.rawHash(), 9));
        when(coordinator.selectContentVersionAppendRequired(SOURCE_ID, preparedClassification))
                .thenReturn(false);
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(
                        invocation.getArgument(0), "COMPLETED", 1, 0, 0, 1, 0
                ));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.duplicateCount()).isEqualTo(1);
        verify(announcementSourceDao, never()).updateSourceSnapshotContent(any());
        verify(coordinator, never()).saveChangedClassification(any(), any(), any(), any(), anyInt());
        verify(announcementSourceDao, never()).insertSourceSnapshot(any());
    }

    /**
     * V2 기능이 꺼져 있어도 기존 의미 판정을 유지하면서 제외 원문과 실행 이력을 보존합니다.
     */
    @Test
    void insertCollectionRunRecordsSemanticallyExcludedItem() {
        AnnouncementSourceProviderItem item = providerItem(
                "LOCAL-IRRELEVANT",
                LocalDate.now().plusDays(10)
        ).withSemanticDecision("EXCLUDED", "NO_INCLUDE_KEYWORD", null);
        AnnouncementSourceClassificationCoordinator coordinator =
                mock(AnnouncementSourceClassificationCoordinator.class);
        AnnouncementSourceClassificationCoordinator.RunContext disabledRunContext =
                AnnouncementSourceClassificationCoordinator.RunContext.disabled();
        when(coordinator.selectRunContext(any(), eq("BIZINFO"))).thenReturn(disabledRunContext);
        when(coordinator.selectClassification(eq(disabledRunContext), any(), any(), any()))
                .thenAnswer(invocation -> AnnouncementSourceClassificationCoordinator.PreparedClassification.disabled(
                        invocation.getArgument(1, AnnouncementSourceProviderItem.class)
                ));
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                localGovernmentNoticeDao,
                announcementDao,
                highlightService,
                List.of(providerClient),
                List.of(),
                coordinator,
                null
        );
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any())).thenReturn(List.of(item));
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> new AnnouncementSourceCollectionRunRow(
                        invocation.getArgument(0), "ASRUN-000001", REQUEST_ID, "ASR-000001",
                        "BIZINFO", "MANUAL", "COMPLETED", NOW, NOW,
                        1, 0, 0, 0, 0, 1, null, NOW
                ));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.excludedCount()).isEqualTo(1);
        ArgumentCaptor<AnnouncementSourceSnapshotCommand> snapshotCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceSnapshotCommand.class);
        ArgumentCaptor<AnnouncementSourceCollectionRunItemCommand> runItemCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceCollectionRunItemCommand.class);
        verify(announcementSourceDao).insertSourceSnapshot(snapshotCaptor.capture());
        verify(announcementSourceDao, never()).insertSourceAttachment(any(AnnouncementSourceAttachmentCommand.class));
        verify(announcementSourceDao).insertCollectionRunItem(runItemCaptor.capture());
        assertThat(snapshotCaptor.getValue().reviewStatusCode()).isEqualTo("ARCHIVED");
        assertThat(snapshotCaptor.getValue().semanticStatusCode()).isEqualTo("EXCLUDED");
        assertThat(snapshotCaptor.getValue().semanticReasonCode()).isEqualTo("NO_INCLUDE_KEYWORD");
        assertThat(runItemCaptor.getValue().itemStatusCode()).isEqualTo("EXCLUDED");
        assertThat(runItemCaptor.getValue().semanticReasonCode()).isEqualTo("NO_INCLUDE_KEYWORD");
        verify(coordinator).selectRunContext(any(), eq("BIZINFO"));
    }

    @Test
    void insertCollectionRunMergesSearchPlanResultsInRoundRobinOrder() {
        AnnouncementSourceClassificationCoordinator coordinator =
                mock(AnnouncementSourceClassificationCoordinator.class);
        AnnouncementSourceClassificationCoordinator.RunContext runContext = searchPlanRunContext();
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                localGovernmentNoticeDao,
                announcementDao,
                highlightService,
                List.of(providerClient),
                List.of(),
                coordinator,
                null
        );
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED", 5));
        when(coordinator.selectRunContext(any(), eq("BIZINFO"))).thenReturn(runContext);
        when(coordinator.selectClassification(any(), any(), any(), any()))
                .thenAnswer(invocation -> new AnnouncementSourceClassificationCoordinator.PreparedClassification(
                        true,
                        runContext.ruleReleaseId(),
                        invocation.getArgument(1, AnnouncementSourceProviderItem.class),
                        null
                ));
        when(providerClient.selectSourceItemList(any())).thenAnswer(invocation -> {
            AnnouncementSourceCollectionRequestRow plannedRequest = invocation.getArgument(0);
            return switch (plannedRequest.searchKeyword()) {
                case "대상1 지원" -> List.of(
                        providerItem("Q1-1", LocalDate.now().plusDays(10)),
                        providerItem("Q1-2", LocalDate.now().plusDays(10)),
                        providerItem("Q1-3", LocalDate.now().plusDays(10))
                );
                case "대상2 지원" -> List.of(
                        providerItem("Q2-1", LocalDate.now().plusDays(10)),
                        providerItem("Q2-2", LocalDate.now().plusDays(10))
                );
                case "대상3 지원" -> List.of(
                        providerItem("Q3-1", LocalDate.now().plusDays(10)),
                        providerItem("Q3-2", LocalDate.now().plusDays(10))
                );
                default -> List.of();
            };
        });
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(
                        invocation.getArgument(0), "COMPLETED", 5, 5, 0, 0, 0
                ));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.collectedCount()).isEqualTo(5);
        ArgumentCaptor<AnnouncementSourceSnapshotCommand> snapshotCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceSnapshotCommand.class);
        verify(announcementSourceDao, times(5)).insertSourceSnapshot(snapshotCaptor.capture());
        verify(announcementSourceDao, never()).insertSourceAttachment(any());
        assertThat(snapshotCaptor.getAllValues())
                .extracting(AnnouncementSourceSnapshotCommand::providerNoticeId)
                .containsExactly("Q1-1", "Q2-1", "Q3-1", "Q1-2", "Q2-2");
    }

    @Test
    void insertCollectionRunFetchesDetailBodyOnlyAfterTitleGateAllowsIt() {
        UUID localSourceId = UUID.fromString("93000000-0000-0000-0000-000000000010");
        AnnouncementSourceProviderClient localProvider = mock(AnnouncementSourceProviderClient.class);
        ProviderContentClient contentClient = mock(ProviderContentClient.class);
        AnnouncementSourceClassificationCoordinator coordinator =
                mock(AnnouncementSourceClassificationCoordinator.class);
        AnnouncementSourceClassificationCoordinator.RunContext runContext =
                new AnnouncementSourceClassificationCoordinator.RunContext(true, UUID.randomUUID(), null, null);
        AnnouncementSourceProviderItem blockedItem = localProviderItem(
                "LOCAL-BLOCKED", "수출기업 모집", localSourceId
        );
        AnnouncementSourceProviderItem fetchItem = localProviderItem(
                "LOCAL-FETCH", "소상공인 지원 본문필요", localSourceId
        );
        when(localProvider.selectProviderCode()).thenReturn("LOCAL_GOV_NOTICE");
        when(contentClient.selectProviderCode()).thenReturn("LOCAL_GOV_NOTICE");
        when(contentClient.isEnabled()).thenReturn(true);
        when(localProvider.selectSourceBatch(any(), any()))
                .thenReturn(AnnouncementSourceProviderBatch.success(List.of(blockedItem, fetchItem)));
        when(coordinator.selectRunContext(any(), eq("LOCAL_GOV_NOTICE"))).thenReturn(runContext);
        when(coordinator.selectBodyFetchRequired(eq(runContext), any()))
                .thenAnswer(invocation -> invocation.getArgument(1, AnnouncementSourceProviderItem.class)
                        .title().contains("본문필요"));
        when(coordinator.selectClassification(any(), any(), any(), any()))
                .thenAnswer(invocation -> AnnouncementSourceClassificationCoordinator.PreparedClassification.disabled(
                        invocation.getArgument(1, AnnouncementSourceProviderItem.class)
                ));
        when(localGovernmentNoticeDao.selectSourceDetails(localSourceId))
                .thenReturn(localGovernmentSource(localSourceId));
        when(contentClient.selectContent(any())).thenAnswer(invocation -> {
            ProviderContentRequest request = invocation.getArgument(0);
            return ProviderContentResult.available(
                    request,
                    "상세 본문",
                    URI.create(request.officialDetailUrl()),
                    200,
                    1,
                    0
            );
        });
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                localGovernmentNoticeDao,
                announcementDao,
                highlightService,
                List.of(localProvider),
                List.of(contentClient),
                coordinator,
                null
        );
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(localGovernmentCollectionRequest(localSourceId));
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(
                        invocation.getArgument(0), "COMPLETED", 2, 2, 0, 0, 0
                ));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.collectedCount()).isEqualTo(2);
        ArgumentCaptor<ProviderContentRequest> contentRequestCaptor =
                ArgumentCaptor.forClass(ProviderContentRequest.class);
        verify(contentClient).selectContent(contentRequestCaptor.capture());
        assertThat(contentRequestCaptor.getValue().officialDetailUrl()).isEqualTo(fetchItem.sourceUrl());
        verify(localGovernmentNoticeDao).selectSourceDetails(localSourceId);
    }

    @Test
    void insertCollectionRunContinuesAfterOneItemTransactionFails() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any()))
                .thenAnswer(ignored -> new SimpleTransactionStatus());
        service = new AnnouncementSourceServiceImpl(
                announcementSourceDao,
                localGovernmentNoticeDao,
                announcementDao,
                highlightService,
                List.of(providerClient),
                List.of(),
                null,
                transactionManager
        );
        AnnouncementSourceProviderItem failedItem = providerItem(
                "BROKEN", LocalDate.now().plusDays(10)
        );
        AnnouncementSourceProviderItem validItem = providerItem(
                "VALID", LocalDate.now().plusDays(10)
        );
        when(announcementSourceDao.selectCollectionRequestDetails(REQUEST_ID))
                .thenReturn(collectionRequest("APPROVED"));
        when(providerClient.selectSourceItemList(any())).thenReturn(List.of(failedItem, validItem));
        doAnswer(invocation -> {
            AnnouncementSourceSnapshotCommand command = invocation.getArgument(0);
            if ("BROKEN".equals(command.providerNoticeId())) {
                throw new IllegalStateException("test item failure");
            }
            return null;
        }).when(announcementSourceDao).insertSourceSnapshot(any());
        when(announcementSourceDao.selectCollectionRunDetails(any()))
                .thenAnswer(invocation -> collectionRun(
                        invocation.getArgument(0), "PARTIAL_FAILED", 2, 1, 0, 0, 1
                ));

        AnnouncementSourceCollectionRunResponse response = service.insertCollectionRun(REQUEST_ID);

        assertThat(response.collectedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(1);
        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
        verify(announcementSourceDao, times(2)).insertSourceSnapshot(any());
        ArgumentCaptor<AnnouncementSourceCollectionRunCommand> runCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceCollectionRunCommand.class);
        verify(announcementSourceDao).updateCollectionRunResult(runCaptor.capture());
        assertThat(runCaptor.getValue().runStatusCode()).isEqualTo("PARTIAL_FAILED");
        assertThat(runCaptor.getValue().collectedCount()).isEqualTo(1);
        assertThat(runCaptor.getValue().failedCount()).isEqualTo(1);
        ArgumentCaptor<AnnouncementSourceCollectionRunItemCommand> runItemCaptor =
                ArgumentCaptor.forClass(AnnouncementSourceCollectionRunItemCommand.class);
        verify(announcementSourceDao, times(2)).insertCollectionRunItem(runItemCaptor.capture());
        assertThat(runItemCaptor.getAllValues())
                .extracting(AnnouncementSourceCollectionRunItemCommand::itemStatusCode)
                .containsExactly("FAILED", "COLLECTED");
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
        verify(announcementSourceDao, never()).insertSourceAttachment(any(AnnouncementSourceAttachmentCommand.class));
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
        when(announcementSourceDao.selectSourceDetailsForUpdate(SOURCE_ID)).thenReturn(sourceRow());
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
     * 이미 연결된 V1 전환 재요청은 기존 연결을 반환하고 쓰기를 반복하지 않습니다.
     */
    @Test
    void insertOperationalAnnouncementReturnsExistingLinkWithoutWrites() {
        UUID announcementId = UUID.fromString("93000000-0000-0000-0000-000000000030");
        when(announcementSourceDao.selectSourceDetailsForUpdate(SOURCE_ID)).thenReturn(sourceRow());
        when(announcementSourceDao.selectLinkedAnnouncementDetails(SOURCE_ID))
                .thenReturn(new AnnouncementSourceLinkedAnnouncementRow(announcementId, "ANN-000030"));

        AnnouncementSourceLinkResponse response = service.insertOperationalAnnouncement(
                authentication(),
                SOURCE_ID,
                new AnnouncementSourceToAnnouncementRequest("BUSINESS", "VAT_TAX_BASE_ONLY")
        );

        assertThat(response).isEqualTo(new AnnouncementSourceLinkResponse(
                SOURCE_ID,
                "SRC-000001",
                announcementId,
                "ANN-000030"
        ));
        verify(announcementDao, never()).insertAnnouncement(any());
        verify(announcementSourceDao, never()).insertSourceLink(any());
        verify(announcementSourceDao, never()).updateSourceReviewStatus(any());
        verify(announcementSourceDao, never()).insertSourceReviewHistory(any());
        verify(announcementSourceDao, never()).insertAuditLog(any());
    }

    /**
     * 이미 연결된 원문의 UPDATE_EXISTING 재처리는 운영 공고와 후보 상태를 변경하지 않습니다.
     */
    @Test
    void updateDuplicateCandidateDecisionRejectsUpdateExistingWhenSourceIsAlreadyLinked() {
        assertLinkedDuplicateDecisionRejectedWithoutWrites("UPDATE_EXISTING");
    }

    /**
     * 이미 연결된 원문의 CREATE_NEW 재처리는 후보 상태와 공고 연결을 변경하지 않습니다.
     */
    @Test
    void updateDuplicateCandidateDecisionRejectsCreateNewWhenSourceIsAlreadyLinked() {
        assertLinkedDuplicateDecisionRejectedWithoutWrites("CREATE_NEW");
    }

    private void assertLinkedDuplicateDecisionRejectedWithoutWrites(String actionCode) {
        UUID candidateId = UUID.fromString("93000000-0000-0000-0000-000000000031");
        UUID announcementId = UUID.fromString("93000000-0000-0000-0000-000000000032");
        when(announcementSourceDao.selectSourceDetailsForUpdate(SOURCE_ID)).thenReturn(sourceRow());
        when(announcementSourceDao.selectLinkedAnnouncementDetails(SOURCE_ID))
                .thenReturn(new AnnouncementSourceLinkedAnnouncementRow(announcementId, "ANN-000032"));

        assertThatThrownBy(() -> service.updateDuplicateCandidateDecision(
                authentication(),
                SOURCE_ID,
                candidateId,
                new AnnouncementSourceDuplicateDecisionRequest(
                        actionCode, "BUSINESS", "VAT_TAX_BASE_ONLY", "중복 검수"
                )
        ))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_SOURCE_NOT_CONVERTIBLE)
                );

        verify(announcementSourceDao, never()).selectDuplicateCandidateDetails(SOURCE_ID, candidateId);
        verify(announcementSourceDao, never()).updateDuplicateCandidateDecision(any());
        verify(announcementSourceDao, never()).insertSourceLink(any());
        verify(announcementSourceDao, never()).updateSourceReviewStatus(any());
        verify(announcementSourceDao, never()).insertSourceReviewHistory(any());
        verify(announcementSourceDao, never()).insertAuditLog(any());
        verifyNoInteractions(announcementDao);
    }

    /**
     * 수집 요청 row를 생성합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private AnnouncementSourceCollectionRequestRow collectionRequest(String statusCode) {
        return collectionRequest(statusCode, 100);
    }

    private AnnouncementSourceCollectionRequestRow collectionRequest(String statusCode, int maximumCount) {
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
                maximumCount,
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

    private AnnouncementSourceClassificationCoordinator.RunContext searchPlanRunContext() {
        UUID releaseId = UUID.fromString("93000000-0000-0000-0000-000000000020");
        AnnouncementSourceSearchPlan searchPlan = new AnnouncementSourceSearchPlan(
                releaseId,
                "BIZINFO",
                AnnouncementSourceSearchPlan.StrategyCode.KEYWORD_COMBINATIONS,
                List.of(
                        new AnnouncementSourceSearchPlan.SearchQuery(
                                1, "TARGET_1", "대상1", "SUPPORT", "지원", "대상1 지원"
                        ),
                        new AnnouncementSourceSearchPlan.SearchQuery(
                                2, "TARGET_2", "대상2", "SUPPORT", "지원", "대상2 지원"
                        ),
                        new AnnouncementSourceSearchPlan.SearchQuery(
                                3, "TARGET_3", "대상3", "SUPPORT", "지원", "대상3 지원"
                        )
                ),
                "{}",
                "a".repeat(64)
        );
        return new AnnouncementSourceClassificationCoordinator.RunContext(
                true, releaseId, null, searchPlan
        );
    }

    private AnnouncementSourceCollectionRequestRow localGovernmentCollectionRequest(UUID sourceId) {
        return new AnnouncementSourceCollectionRequestRow(
                REQUEST_ID,
                "ASR-000001",
                "LOCAL_GOV_NOTICE",
                "MANUAL",
                "APPROVED",
                UUID.randomUUID(),
                NOW,
                "ADMIN_BUTTON",
                null,
                null,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                10,
                "요청",
                sourceId,
                null,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    private AnnouncementSourceProviderItem localProviderItem(
            String providerNoticeId,
            String title,
            UUID sourceId
    ) {
        return new AnnouncementSourceProviderItem(
                "LOCAL_GOV_NOTICE",
                providerNoticeId,
                title,
                "테스트구청",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                NOW,
                NOW,
                "https://example.go.kr/notice/" + providerNoticeId,
                null,
                null,
                null,
                "PARTIAL",
                "{}",
                "{\"title\":\"" + title + "\"}",
                "hash-" + providerNoticeId,
                List.of(),
                sourceId
        );
    }

    private LocalGovernmentNoticeSourceRow localGovernmentSource(UUID sourceId) {
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
                0,
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
        return sourceRow("hash-BIZ-DUP", 0);
    }

    private AnnouncementSourceSnapshotRow sourceRow(String rawHash, int classificationRowVersion) {
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
                rawHash,
                "REVIEW_PENDING",
                "ACCEPTED",
                "PROVIDER_TRUSTED",
                null,
                classificationRowVersion,
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

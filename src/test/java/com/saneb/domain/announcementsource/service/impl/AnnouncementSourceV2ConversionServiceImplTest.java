/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceV2ConversionServiceImplTest.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementSupportTypeAssignmentCommand;
import com.saneb.domain.announcement.vo.AnnouncementTargetCategoryAssignmentCommand;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceDao;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceV2ToAnnouncementRequest;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationStateRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceLinkedAnnouncementRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceSnapshotRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AnnouncementSourceV2ConversionServiceImplTest {

    private static final UUID SOURCE_ID = UUID.fromString("94000000-0000-0000-0000-000000000001");
    private static final UUID DECISION_ID = UUID.fromString("94000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-12T10:00:00+09:00");

    @Mock
    private AnnouncementSourceDao announcementSourceDao;

    @Mock
    private AnnouncementSourceClassificationDao classificationDao;

    @Mock
    private AnnouncementDao announcementDao;

    private AnnouncementSourceV2ConversionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementSourceV2ConversionServiceImpl(
                announcementSourceDao,
                classificationDao,
                announcementDao
        );
    }

    @Test
    void insertOperationalAnnouncementReturnsExistingLinkWithoutAnyWrite() {
        UUID announcementId = UUID.fromString("94000000-0000-0000-0000-000000000003");
        when(announcementSourceDao.selectSourceDetailsForUpdate(SOURCE_ID)).thenReturn(sourceRow());
        when(announcementSourceDao.selectLinkedAnnouncementDetails(SOURCE_ID))
                .thenReturn(new AnnouncementSourceLinkedAnnouncementRow(announcementId, "ANN-000003"));

        AnnouncementSourceLinkResponse response = service.insertOperationalAnnouncement(
                authentication(), SOURCE_ID, request()
        );

        assertThat(response).isEqualTo(new AnnouncementSourceLinkResponse(
                SOURCE_ID, "SRC-000001", announcementId, "ANN-000003"
        ));
        InOrder readOrder = inOrder(announcementSourceDao);
        readOrder.verify(announcementSourceDao).selectSourceDetailsForUpdate(SOURCE_ID);
        readOrder.verify(announcementSourceDao).selectLinkedAnnouncementDetails(SOURCE_ID);
        verifyNoInteractions(classificationDao, announcementDao);
        verify(announcementSourceDao, never()).insertSourceLink(any());
        verify(announcementSourceDao, never()).updateSourceReviewStatus(any());
        verify(announcementSourceDao, never()).insertSourceReviewHistory(any());
        verify(announcementSourceDao, never()).insertAuditLog(any());
    }

    @Test
    void insertOperationalAnnouncementCreatesOneDraftAndConfirmedAssignments() {
        when(announcementSourceDao.selectSourceDetailsForUpdate(SOURCE_ID)).thenReturn(sourceRow());
        when(classificationDao.selectClassificationStateDetails(SOURCE_ID)).thenReturn(confirmedState(3));
        when(classificationDao.selectConfirmedTargetCategoryCodeList(SOURCE_ID))
                .thenReturn(List.of("BUSINESS", "PERSONAL"));
        when(classificationDao.selectConfirmedSupportTypeCodeList(SOURCE_ID))
                .thenReturn(List.of("GENERAL_SUPPORT", "POLICY_FINANCE"));
        when(announcementDao.selectAnnouncementDetails(any())).thenAnswer(invocation ->
                announcementRow(invocation.getArgument(0))
        );

        AnnouncementSourceLinkResponse response = service.insertOperationalAnnouncement(
                authentication(), SOURCE_ID, request()
        );

        assertThat(response.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(response.sourcePublicCode()).isEqualTo("SRC-000001");
        assertThat(response.announcementCode()).isEqualTo("ANN-000100");
        verify(announcementDao).insertAnnouncement(any());
        verify(announcementDao, times(2)).insertAnnouncementTargetCategoryAssignment(
                any(AnnouncementTargetCategoryAssignmentCommand.class)
        );
        verify(announcementDao, times(2)).insertAnnouncementSupportTypeAssignment(
                any(AnnouncementSupportTypeAssignmentCommand.class)
        );
        verify(announcementSourceDao).insertSourceLink(any());
        verify(announcementSourceDao).updateSourceReviewStatus(any());
        verify(announcementSourceDao).insertSourceReviewHistory(any());
        verify(announcementSourceDao).insertAuditLog(any());
    }

    @Test
    void insertOperationalAnnouncementRejectsStaleClassificationVersionBeforeWrites() {
        when(announcementSourceDao.selectSourceDetailsForUpdate(SOURCE_ID)).thenReturn(sourceRow());
        when(classificationDao.selectClassificationStateDetails(SOURCE_ID)).thenReturn(confirmedState(4));

        assertThatThrownBy(() -> service.insertOperationalAnnouncement(authentication(), SOURCE_ID, request()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT)
                );

        verifyNoInteractions(announcementDao);
        verify(announcementSourceDao, never()).insertSourceLink(any());
        verify(announcementSourceDao, never()).updateSourceReviewStatus(any());
        verify(announcementSourceDao, never()).insertSourceReviewHistory(any());
        verify(announcementSourceDao, never()).insertAuditLog(any());
    }

    @Test
    void insertOperationalAnnouncementRequiresCurrentConfirmedCategoriesBeforeWrites() {
        when(announcementSourceDao.selectSourceDetailsForUpdate(SOURCE_ID)).thenReturn(sourceRow());
        when(classificationDao.selectClassificationStateDetails(SOURCE_ID)).thenReturn(
                new AnnouncementSourceClassificationStateRow(
                        SOURCE_ID, DECISION_ID, "ACCEPTED", "TARGET_SUPPORT_MATCH", "REVIEW_PENDING",
                        3, 1L, 1L, 1L
                )
        );

        assertThatThrownBy(() -> service.insertOperationalAnnouncement(authentication(), SOURCE_ID, request()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(
                                ErrorCode.ANNOUNCEMENT_SOURCE_CLASSIFICATION_REQUIRED
                        )
                );

        verifyNoInteractions(announcementDao);
        verify(announcementSourceDao, never()).insertSourceLink(any());
        verify(announcementSourceDao, never()).updateSourceReviewStatus(any());
        verify(announcementSourceDao, never()).insertSourceReviewHistory(any());
        verify(announcementSourceDao, never()).insertAuditLog(any());
    }

    private AnnouncementSourceV2ToAnnouncementRequest request() {
        return new AnnouncementSourceV2ToAnnouncementRequest(
                "BUSINESS",
                List.of("BUSINESS", "PERSONAL"),
                List.of("GENERAL_SUPPORT", "POLICY_FINANCE"),
                "VAT_TAX_BASE_ONLY",
                DECISION_ID,
                3
        );
    }

    private AnnouncementSourceClassificationStateRow confirmedState(int version) {
        return new AnnouncementSourceClassificationStateRow(
                SOURCE_ID,
                DECISION_ID,
                "ACCEPTED",
                "TARGET_SUPPORT_MATCH",
                "REVIEW_PENDING",
                version,
                2L,
                2L,
                0L
        );
    }

    private AnnouncementSourceSnapshotRow sourceRow() {
        return new AnnouncementSourceSnapshotRow(
                SOURCE_ID,
                "SRC-000001",
                "BIZINFO",
                "BIZ-000001",
                "소상공인 정책자금 지원",
                "중소벤처기업부",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                NOW,
                NOW,
                "https://example.com/announcements/1",
                "지원사업 본문",
                "문의처",
                "온라인 신청",
                "COMPLETE",
                "{}",
                "raw-hash",
                "REVIEW_PENDING",
                "ACCEPTED",
                "TARGET_SUPPORT_MATCH",
                "대상/지원형태 일치",
                3,
                NOW,
                NOW,
                NOW
        );
    }

    private AnnouncementDetailsRow announcementRow(UUID announcementId) {
        return new AnnouncementDetailsRow(
                announcementId,
                "ANN-000100",
                "BUSINESS",
                "소상공인 정책자금 지원",
                "중소벤처기업부",
                "지원사업 본문",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "NORMAL",
                "DRAFT",
                "VAT_TAX_BASE_ONLY",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                NOW,
                NOW
        );
    }

    private Authentication authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.fromString("94000000-0000-0000-0000-000000000010"),
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

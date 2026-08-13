/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementServiceImplTest.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.dto.AnnouncementDetailsResponse;
import com.saneb.domain.announcement.dto.AnnouncementV2SaveRequest;
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementSaveCommand;
import com.saneb.domain.announcement.vo.AnnouncementSupportTypeAssignmentCommand;
import com.saneb.domain.announcement.vo.AnnouncementTargetCategoryAssignmentCommand;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceImplTest {

    private static final UUID ANNOUNCEMENT_ID =
            UUID.fromString("98000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-12T10:00:00+09:00");

    @Mock
    private AnnouncementDao announcementDao;

    private AnnouncementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementServiceImpl(announcementDao);
    }

    @Test
    void insertAnnouncementV2PersistsPrimaryAndMultipleClassificationTags() {
        stubDetailsQueries();
        AnnouncementV2SaveRequest request = new AnnouncementV2SaveRequest(
                "BUSINESS",
                List.of("BUSINESS", "PERSONAL"),
                List.of("POLICY_FINANCE", "INTEREST_SUPPORT"),
                "소상공인 정책자금 지원",
                "중소벤처기업부",
                "지원사업 본문",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "VAT_TAX_BASE_ONLY",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        );

        AnnouncementDetailsResponse response = service.insertAnnouncementV2(authentication(), request);

        ArgumentCaptor<AnnouncementSaveCommand> announcementCaptor =
                ArgumentCaptor.forClass(AnnouncementSaveCommand.class);
        ArgumentCaptor<AnnouncementTargetCategoryAssignmentCommand> targetCaptor =
                ArgumentCaptor.forClass(AnnouncementTargetCategoryAssignmentCommand.class);
        ArgumentCaptor<AnnouncementSupportTypeAssignmentCommand> supportCaptor =
                ArgumentCaptor.forClass(AnnouncementSupportTypeAssignmentCommand.class);
        verify(announcementDao).insertAnnouncement(announcementCaptor.capture());
        verify(announcementDao, times(2)).insertAnnouncementTargetCategoryAssignment(targetCaptor.capture());
        verify(announcementDao, times(2)).insertAnnouncementSupportTypeAssignment(supportCaptor.capture());

        UUID generatedAnnouncementId = announcementCaptor.getValue().id();
        assertThat(targetCaptor.getAllValues())
                .extracting(AnnouncementTargetCategoryAssignmentCommand::announcementId)
                .containsOnly(generatedAnnouncementId);
        assertThat(targetCaptor.getAllValues())
                .extracting(AnnouncementTargetCategoryAssignmentCommand::targetCategoryCode)
                .containsExactly("BUSINESS", "PERSONAL");
        assertThat(targetCaptor.getAllValues())
                .extracting(AnnouncementTargetCategoryAssignmentCommand::primary)
                .containsExactly(true, false);
        assertThat(targetCaptor.getAllValues())
                .extracting(AnnouncementTargetCategoryAssignmentCommand::assignmentSourceCode)
                .containsOnly("MANUAL");
        assertThat(supportCaptor.getAllValues())
                .extracting(AnnouncementSupportTypeAssignmentCommand::announcementId)
                .containsOnly(generatedAnnouncementId);
        assertThat(supportCaptor.getAllValues())
                .extracting(AnnouncementSupportTypeAssignmentCommand::supportTypeCode)
                .containsExactly("POLICY_FINANCE", "INTEREST_SUPPORT");
        assertThat(response.targetCategoryCodes()).containsExactly("BUSINESS", "PERSONAL");
        assertThat(response.supportTypeCodes()).containsExactly("POLICY_FINANCE", "INTEREST_SUPPORT");
    }

    private void stubDetailsQueries() {
        when(announcementDao.selectAnnouncementDetails(any())).thenReturn(new AnnouncementDetailsRow(
                ANNOUNCEMENT_ID,
                "ANN-000001",
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
        ));
        when(announcementDao.selectAnnouncementTargetCategoryCodeList(any()))
                .thenReturn(List.of("BUSINESS", "PERSONAL"));
        when(announcementDao.selectAnnouncementSupportTypeCodeList(any()))
                .thenReturn(List.of("POLICY_FINANCE", "INTEREST_SUPPORT"));
        when(announcementDao.selectAnnouncementOptionList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementIndustryConditionList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementNumericConditionList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementOptionConditionList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementDocumentRequirementList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementProgressStepList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementStepDocumentList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementStepButtonList(any())).thenReturn(List.of());
    }

    private Authentication authentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.fromString("98000000-0000-0000-0000-000000000002"),
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

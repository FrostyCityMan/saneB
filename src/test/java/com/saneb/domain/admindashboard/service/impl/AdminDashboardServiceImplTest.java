/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminDashboardServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.admindashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.saneb.domain.admindashboard.dao.AdminDashboardDao;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse;
import com.saneb.domain.admindashboard.vo.AdminApplicationProgressSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminAnnouncementSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminAuditSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminStatusCountRow;
import com.saneb.domain.admindashboard.vo.AdminUserSummaryRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private AdminDashboardDao adminDashboardDao;

    private AdminDashboardServiceImpl adminDashboardService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardServiceImpl(adminDashboardDao);
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectSummaryAggregatesOperationalCountsWithoutSensitiveValues() {
        when(adminDashboardDao.selectUserSummary()).thenReturn(
                new AdminUserSummaryRow(10, 9, 6, 1, 1, 1, 1)
        );
        when(adminDashboardDao.selectAnnouncementSummary()).thenReturn(
                new AdminAnnouncementSummaryRow(20, 3, 2, 12, 1, 8, 1, 6)
        );
        when(adminDashboardDao.selectPartnerVerificationStatusCountList()).thenReturn(List.of(
                new AdminStatusCountRow("SUBMITTED", 2),
                new AdminStatusCountRow("REVIEWING", 3),
                new AdminStatusCountRow("VERIFIED", 9)
        ));
        when(adminDashboardDao.selectMatchingCaseStatusCountList()).thenReturn(List.of(
                new AdminStatusCountRow("MATCHED", 12),
                new AdminStatusCountRow("REVIEW_REQUIRED", 2),
                new AdminStatusCountRow("PROGRESSED", 7)
        ));
        when(adminDashboardDao.selectApplicationProgressSummary()).thenReturn(
                new AdminApplicationProgressSummaryRow(
                        8,
                        1,
                        2,
                        1,
                        2,
                        1,
                        0,
                        0,
                        1,
                        new BigDecimal("5000000.00")
                )
        );
        when(adminDashboardDao.selectAuditSummary()).thenReturn(new AdminAuditSummaryRow(100, 4, 1));

        AdminDashboardSummaryResponse response = adminDashboardService.selectSummary();

        assertThat(response.userSummary().totalUserCount()).isEqualTo(10);
        assertThat(response.announcementSummary().openAnnouncementCount()).isEqualTo(8);
        assertThat(response.verificationSummary().reviewQueueCount()).isEqualTo(5);
        assertThat(response.verificationSummary().statusCounts())
                .extracting(AdminDashboardSummaryResponse.StatusCountResponse::statusCode)
                .containsExactly("DRAFT", "SUBMITTED", "REVIEWING", "VERIFIED", "REJECTED", "EXPIRED");
        assertThat(response.matchingSummary().reviewRequiredCount()).isEqualTo(2);
        assertThat(response.applicationProgressSummary().activeProgressCount()).isEqualTo(3);
        assertThat(response.applicationProgressSummary().totalReceivedAmount()).isEqualByComparingTo("5000000.00");
        assertThat(response.auditSummary().recentFailAuditCount()).isEqualTo(1);
    }
}

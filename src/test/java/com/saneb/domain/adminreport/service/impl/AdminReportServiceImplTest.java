/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminReportServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.adminreport.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.adminreport.dao.AdminReportDao;
import com.saneb.domain.adminreport.dto.ReportExportCreateRequest;
import com.saneb.domain.adminreport.vo.AdminReportSummaryRow;
import com.saneb.domain.adminreport.vo.ReportExportInsertCommand;
import com.saneb.domain.adminreport.vo.ReportExportRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
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

@ExtendWith(MockitoExtension.class)
class AdminReportServiceImplTest {

    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EXPORT_ID = UUID.fromString("92000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Mock
    private AdminReportDao adminReportDao;

    private AdminReportServiceImpl adminReportService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        adminReportService = new AdminReportServiceImpl(adminReportDao);
    }

    /**
     * 업무 데이터를 등록합니다.
     */
    @Test
    void insertReportExportCreatesCompletedCsvExportAndSnapshot() {
        when(adminReportDao.selectAdminReportSummary()).thenReturn(summary());
        when(adminReportDao.selectReportExportDetails(any())).thenReturn(export());

        var response = adminReportService.insertReportExport(
                authentication(List.of("ADMIN")),
                new ReportExportCreateRequest("operation_summary", "csv")
        );

        ArgumentCaptor<ReportExportInsertCommand> captor = ArgumentCaptor.forClass(ReportExportInsertCommand.class);
        verify(adminReportDao).insertReportExport(captor.capture());
        assertThat(captor.getValue().reportTypeCode()).isEqualTo("OPERATION_SUMMARY");
        assertThat(captor.getValue().formatCode()).isEqualTo("CSV");
        assertThat(captor.getValue().statusCode()).isEqualTo("COMPLETED");
        assertThat(captor.getValue().contentText()).contains("totalUserCount");
        verify(adminReportDao).insertAdminReportSnapshot(any());
        verify(adminReportDao).insertAuditLog(any());
        assertThat(response.statusCode()).isEqualTo("COMPLETED");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectAdminReportSummaryRejectsNonAdmin() {
        assertThatThrownBy(() -> adminReportService.selectAdminReportSummary(authentication(List.of("USER"))))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectReportExportDownloadRejectsUncompletedExport() {
        when(adminReportDao.selectReportExportDetails(EXPORT_ID)).thenReturn(new ReportExportRow(
                EXPORT_ID,
                "OPERATION_SUMMARY",
                "CSV",
                "REQUESTED",
                ADMIN_ID,
                0,
                null,
                null,
                CREATED_AT,
                null,
                null,
                null
        ));

        assertThatThrownBy(() -> adminReportService.selectReportExportDownload(authentication(List.of("ADMIN")), EXPORT_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.PROGRESS_CONDITION_NOT_MET);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AdminReportSummaryRow summary() {
        return new AdminReportSummaryRow(10, 8, 3, 2, 5, 4, 7, 6, 2, 1, new BigDecimal("99000.00"), 3, 4);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private ReportExportRow export() {
        return new ReportExportRow(
                EXPORT_ID,
                "OPERATION_SUMMARY",
                "CSV",
                "COMPLETED",
                ADMIN_ID,
                1,
                "operation-summary.csv",
                "a,b\n1,2\n",
                CREATED_AT,
                CREATED_AT,
                null,
                null
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
    private UsernamePasswordAuthenticationToken authentication(List<String> roles) {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        ADMIN_ID,
                        "admin",
                        "hash",
                        "Admin User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}

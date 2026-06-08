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

    @BeforeEach
    void setUp() {
        adminReportService = new AdminReportServiceImpl(adminReportDao);
    }

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

    @Test
    void selectAdminReportSummaryRejectsNonAdmin() {
        assertThatThrownBy(() -> adminReportService.selectAdminReportSummary(authentication(List.of("USER"))))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

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

    private AdminReportSummaryRow summary() {
        return new AdminReportSummaryRow(10, 8, 3, 2, 5, 4, 7, 6, 2, 1, new BigDecimal("99000.00"), 3, 4);
    }

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

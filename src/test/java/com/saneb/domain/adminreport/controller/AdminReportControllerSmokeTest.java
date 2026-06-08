package com.saneb.domain.adminreport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.adminreport.dto.AdminReportSummaryResponse;
import com.saneb.domain.adminreport.dto.ReportExportDownloadResponse;
import com.saneb.domain.adminreport.dto.ReportExportResponse;
import com.saneb.domain.adminreport.service.AdminReportService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AdminReportControllerSmokeTest {

    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EXPORT_ID = UUID.fromString("92000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReportService adminReportService;

    @BeforeEach
    void setUp() {
        when(adminReportService.selectAdminReportSummary(any())).thenReturn(summary());
        when(adminReportService.insertReportExport(any(), any())).thenReturn(export());
        when(adminReportService.selectReportExportDetails(any(), eq(EXPORT_ID))).thenReturn(export());
        when(adminReportService.selectReportExportDownload(any(), eq(EXPORT_ID))).thenReturn(download());
    }

    @Test
    void selectAdminReportSummaryReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUserCount").value(10));
    }

    @Test
    void selectAdminReportSummaryRejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .with(user(userPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void insertReportExportReturnsApiResponse() throws Exception {
        mockMvc.perform(post("/api/v1/admin/reports/exports")
                        .with(user(adminPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportTypeCode": "OPERATION_SUMMARY",
                                  "formatCode": "CSV"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportId").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("COMPLETED"));
    }

    @Test
    void selectReportExportDownloadReturnsWrappedContent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/exports/{exportId}/download", EXPORT_ID)
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("operation_summary.csv"))
                .andExpect(jsonPath("$.data.content").value("a,b\n1,2\n"));
    }

    private AdminReportSummaryResponse summary() {
        return new AdminReportSummaryResponse(10, 8, 3, 2, 5, 4, 7, 6, 2, 1, new BigDecimal("99000.00"), 3, 4);
    }

    private ReportExportResponse export() {
        return new ReportExportResponse(
                EXPORT_ID,
                "OPERATION_SUMMARY",
                "CSV",
                "COMPLETED",
                ADMIN_ID,
                1,
                "operation_summary.csv",
                CREATED_AT,
                CREATED_AT,
                null,
                null
        );
    }

    private ReportExportDownloadResponse download() {
        return new ReportExportDownloadResponse(EXPORT_ID, "operation_summary.csv", "text/csv;charset=UTF-8", "a,b\n1,2\n");
    }

    private AuthenticatedUserDetails adminPrincipal() {
        return principal(List.of("ADMIN"));
    }

    private AuthenticatedUserDetails userPrincipal() {
        return principal(List.of("USER"));
    }

    private AuthenticatedUserDetails principal(List<String> roles) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        ADMIN_ID,
                        "admin",
                        "password-hash",
                        "Admin User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
    }
}

package com.saneb.domain.adminreport.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.adminreport.dto.AdminReportSummaryResponse;
import com.saneb.domain.adminreport.dto.ReportExportCreateRequest;
import com.saneb.domain.adminreport.dto.ReportExportDownloadResponse;
import com.saneb.domain.adminreport.dto.ReportExportResponse;
import com.saneb.domain.adminreport.service.AdminReportService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminReportSummaryResponse> selectAdminReportSummary(Authentication authentication) {
        return ApiResponse.success(adminReportService.selectAdminReportSummary(authentication));
    }

    @PostMapping("/exports")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportExportResponse> insertReportExport(
            Authentication authentication,
            @Valid @RequestBody ReportExportCreateRequest request
    ) {
        return ApiResponse.success(adminReportService.insertReportExport(authentication, request));
    }

    @GetMapping("/exports/{exportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportExportResponse> selectReportExportDetails(
            Authentication authentication,
            @PathVariable UUID exportId
    ) {
        return ApiResponse.success(adminReportService.selectReportExportDetails(authentication, exportId));
    }

    @GetMapping("/exports/{exportId}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportExportDownloadResponse> selectReportExportDownload(
            Authentication authentication,
            @PathVariable UUID exportId
    ) {
        return ApiResponse.success(adminReportService.selectReportExportDownload(authentication, exportId));
    }
}

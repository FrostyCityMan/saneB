package com.saneb.domain.adminreport.service;

import com.saneb.domain.adminreport.dto.AdminReportSummaryResponse;
import com.saneb.domain.adminreport.dto.ReportExportCreateRequest;
import com.saneb.domain.adminreport.dto.ReportExportDownloadResponse;
import com.saneb.domain.adminreport.dto.ReportExportResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AdminReportService {

    AdminReportSummaryResponse selectAdminReportSummary(Authentication authentication);

    ReportExportResponse insertReportExport(Authentication authentication, ReportExportCreateRequest request);

    ReportExportResponse selectReportExportDetails(Authentication authentication, UUID exportId);

    ReportExportDownloadResponse selectReportExportDownload(Authentication authentication, UUID exportId);
}

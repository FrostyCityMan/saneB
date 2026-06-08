package com.saneb.domain.adminreport.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.adminreport.dao.AdminReportDao;
import com.saneb.domain.adminreport.dto.AdminReportSummaryResponse;
import com.saneb.domain.adminreport.dto.ReportExportCreateRequest;
import com.saneb.domain.adminreport.dto.ReportExportDownloadResponse;
import com.saneb.domain.adminreport.dto.ReportExportResponse;
import com.saneb.domain.adminreport.service.AdminReportService;
import com.saneb.domain.adminreport.vo.AdminReportSnapshotInsertCommand;
import com.saneb.domain.adminreport.vo.AdminReportSummaryRow;
import com.saneb.domain.adminreport.vo.AuditLogCommand;
import com.saneb.domain.adminreport.vo.ReportExportInsertCommand;
import com.saneb.domain.adminreport.vo.ReportExportRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportServiceImpl implements AdminReportService {

    private static final Set<String> REPORT_TYPE_CODES = Set.of("OPERATION_SUMMARY");
    private static final Set<String> FORMAT_CODES = Set.of("CSV", "EXCEL");

    private final AdminReportDao adminReportDao;

    public AdminReportServiceImpl(AdminReportDao adminReportDao) {
        this.adminReportDao = adminReportDao;
    }

    @Override
    public AdminReportSummaryResponse selectAdminReportSummary(Authentication authentication) {
        selectRequiredAdminPrincipal(authentication);
        return toSummaryResponse(selectSummaryRow());
    }

    @Override
    @Transactional
    public ReportExportResponse insertReportExport(Authentication authentication, ReportExportCreateRequest request) {
        AuthenticatedUserDetails actor = selectRequiredAdminPrincipal(authentication);
        String reportTypeCode = normalizeRequiredCode("reportTypeCode", request.reportTypeCode(), REPORT_TYPE_CODES);
        String formatCode = normalizeRequiredCode("formatCode", request.formatCode(), FORMAT_CODES);
        AdminReportSummaryRow summary = selectSummaryRow();
        String content = buildSummaryExportContent(summary, formatCode);
        UUID exportId = UUID.randomUUID();
        String fileName = buildFileName(reportTypeCode, formatCode);
        adminReportDao.insertReportExport(new ReportExportInsertCommand(
                exportId,
                reportTypeCode,
                formatCode,
                "COMPLETED",
                actor.userId(),
                1,
                fileName,
                content
        ));
        adminReportDao.insertAdminReportSnapshot(new AdminReportSnapshotInsertCommand(
                UUID.randomUUID(),
                reportTypeCode,
                summaryJson(summary),
                actor.userId()
        ));
        insertAudit(actor.userId(), exportId, metadata(
                "reportTypeCode", reportTypeCode,
                "formatCode", formatCode,
                "rowCount", "1"
        ));
        return toExportResponse(selectExportRow(exportId));
    }

    @Override
    public ReportExportResponse selectReportExportDetails(Authentication authentication, UUID exportId) {
        selectRequiredAdminPrincipal(authentication);
        return toExportResponse(selectExportRow(exportId));
    }

    @Override
    public ReportExportDownloadResponse selectReportExportDownload(Authentication authentication, UUID exportId) {
        selectRequiredAdminPrincipal(authentication);
        ReportExportRow row = selectExportRow(exportId);
        if (!"COMPLETED".equals(row.statusCode()) || row.contentText() == null) {
            throw new ApiException(ErrorCode.PROGRESS_CONDITION_NOT_MET, HttpStatus.CONFLICT, "다운로드할 수 없는 리포트 상태입니다.");
        }
        return new ReportExportDownloadResponse(
                row.exportId(),
                row.fileName(),
                "CSV".equals(row.formatCode()) ? "text/csv;charset=UTF-8" : "text/tab-separated-values;charset=UTF-8",
                row.contentText()
        );
    }

    private AdminReportSummaryRow selectSummaryRow() {
        AdminReportSummaryRow row = adminReportDao.selectAdminReportSummary();
        if (row == null) {
            return new AdminReportSummaryRow(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, 0, 0);
        }
        return row;
    }

    private ReportExportRow selectExportRow(UUID exportId) {
        ReportExportRow row = adminReportDao.selectReportExportDetails(exportId);
        if (row == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "리포트 내보내기 내역을 찾을 수 없습니다.");
        }
        return row;
    }

    private String buildSummaryExportContent(AdminReportSummaryRow row, String formatCode) {
        String delimiter = "CSV".equals(formatCode) ? "," : "\t";
        return String.join(delimiter,
                "totalUserCount",
                "activeUserCount",
                "approvedAnnouncementCount",
                "openAnnouncementCount",
                "matchedCaseCount",
                "progressedCaseCount",
                "activeProgressCount",
                "completedProgressCount",
                "activeSubscriptionCount",
                "approvedPaymentCount",
                "approvedPaymentAmount",
                "openOperationTaskCount",
                "unreadNotificationCount"
        ) + "\n" + String.join(delimiter,
                String.valueOf(row.totalUserCount()),
                String.valueOf(row.activeUserCount()),
                String.valueOf(row.approvedAnnouncementCount()),
                String.valueOf(row.openAnnouncementCount()),
                String.valueOf(row.matchedCaseCount()),
                String.valueOf(row.progressedCaseCount()),
                String.valueOf(row.activeProgressCount()),
                String.valueOf(row.completedProgressCount()),
                String.valueOf(row.activeSubscriptionCount()),
                String.valueOf(row.approvedPaymentCount()),
                row.approvedPaymentAmount().toPlainString(),
                String.valueOf(row.openOperationTaskCount()),
                String.valueOf(row.unreadNotificationCount())
        ) + "\n";
    }

    private String buildFileName(String reportTypeCode, String formatCode) {
        String suffix = "CSV".equals(formatCode) ? ".csv" : ".tsv";
        return reportTypeCode.toLowerCase(Locale.ROOT) + "-"
                + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + suffix;
    }

    private String summaryJson(AdminReportSummaryRow row) {
        return "{"
                + "\"totalUserCount\":" + row.totalUserCount() + ","
                + "\"activeUserCount\":" + row.activeUserCount() + ","
                + "\"approvedAnnouncementCount\":" + row.approvedAnnouncementCount() + ","
                + "\"openAnnouncementCount\":" + row.openAnnouncementCount() + ","
                + "\"matchedCaseCount\":" + row.matchedCaseCount() + ","
                + "\"progressedCaseCount\":" + row.progressedCaseCount() + ","
                + "\"activeProgressCount\":" + row.activeProgressCount() + ","
                + "\"completedProgressCount\":" + row.completedProgressCount() + ","
                + "\"activeSubscriptionCount\":" + row.activeSubscriptionCount() + ","
                + "\"approvedPaymentCount\":" + row.approvedPaymentCount() + ","
                + "\"approvedPaymentAmount\":\"" + row.approvedPaymentAmount().toPlainString() + "\","
                + "\"openOperationTaskCount\":" + row.openOperationTaskCount() + ","
                + "\"unreadNotificationCount\":" + row.unreadNotificationCount()
                + "}";
    }

    private AuthenticatedUserDetails selectRequiredAdminPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal
                && principal.roles().contains("ADMIN")) {
            return principal;
        }
        throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String code = value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
        if (code == null || !allowedValues.contains(code)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, fieldName + " 값이 올바르지 않습니다.");
        }
        return code;
    }

    private AdminReportSummaryResponse toSummaryResponse(AdminReportSummaryRow row) {
        return new AdminReportSummaryResponse(
                row.totalUserCount(),
                row.activeUserCount(),
                row.approvedAnnouncementCount(),
                row.openAnnouncementCount(),
                row.matchedCaseCount(),
                row.progressedCaseCount(),
                row.activeProgressCount(),
                row.completedProgressCount(),
                row.activeSubscriptionCount(),
                row.approvedPaymentCount(),
                row.approvedPaymentAmount(),
                row.openOperationTaskCount(),
                row.unreadNotificationCount()
        );
    }

    private ReportExportResponse toExportResponse(ReportExportRow row) {
        return new ReportExportResponse(
                row.exportId(),
                row.reportTypeCode(),
                row.formatCode(),
                row.statusCode(),
                row.requestedBy(),
                row.rowCount(),
                row.fileName(),
                row.requestedAt(),
                row.completedAt(),
                row.failureCode(),
                row.failureMessage()
        );
    }

    private void insertAudit(UUID actorUserId, UUID exportId, String metadataJson) {
        adminReportDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                "REPORT_EXPORT_CREATE",
                "REPORT_EXPORT",
                exportId,
                "SUCCESS",
                metadataJson
        ));
    }

    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

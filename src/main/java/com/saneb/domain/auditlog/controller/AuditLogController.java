package com.saneb.domain.auditlog.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auditlog.dto.AuditLogDetailsResponse;
import com.saneb.domain.auditlog.dto.AuditLogSummaryResponse;
import com.saneb.domain.auditlog.service.AuditLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ApiResponse<PageResponse<AuditLogSummaryResponse>> selectAuditLogList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resultCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(auditLogService.selectAuditLogList(
                keyword,
                actionCode,
                resourceType,
                resultCode,
                page,
                size
        ));
    }

    @GetMapping("/{auditLogId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ApiResponse<AuditLogDetailsResponse> selectAuditLogDetails(@PathVariable UUID auditLogId) {
        return ApiResponse.success(auditLogService.selectAuditLogDetails(auditLogId));
    }
}

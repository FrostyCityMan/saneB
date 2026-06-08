package com.saneb.domain.auditlog.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auditlog.dto.AuditLogDetailsResponse;
import com.saneb.domain.auditlog.dto.AuditLogSummaryResponse;
import java.util.UUID;

public interface AuditLogService {

    PageResponse<AuditLogSummaryResponse> selectAuditLogList(
            String keyword,
            String actionCode,
            String resourceType,
            String resultCode,
            int page,
            int size
    );

    AuditLogDetailsResponse selectAuditLogDetails(UUID auditLogId);
}

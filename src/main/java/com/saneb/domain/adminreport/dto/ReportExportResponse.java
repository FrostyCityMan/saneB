package com.saneb.domain.adminreport.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportExportResponse(
        UUID exportId,
        String reportTypeCode,
        String formatCode,
        String statusCode,
        UUID requestedBy,
        int rowCount,
        String fileName,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt,
        String failureCode,
        String failureMessage
) {
}

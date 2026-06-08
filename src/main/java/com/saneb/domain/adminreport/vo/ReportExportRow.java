package com.saneb.domain.adminreport.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportExportRow(
        UUID exportId,
        String reportTypeCode,
        String formatCode,
        String statusCode,
        UUID requestedBy,
        int rowCount,
        String fileName,
        String contentText,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt,
        String failureCode,
        String failureMessage
) {
}

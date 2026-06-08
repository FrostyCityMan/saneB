package com.saneb.domain.adminreport.vo;

import java.util.UUID;

public record ReportExportInsertCommand(
        UUID exportId,
        String reportTypeCode,
        String formatCode,
        String statusCode,
        UUID requestedBy,
        int rowCount,
        String fileName,
        String contentText
) {
}

package com.saneb.domain.adminreport.dto;

import java.util.UUID;

public record ReportExportDownloadResponse(
        UUID exportId,
        String fileName,
        String contentType,
        String content
) {
}

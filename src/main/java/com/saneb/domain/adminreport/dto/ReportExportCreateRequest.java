package com.saneb.domain.adminreport.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportExportCreateRequest(
        @NotBlank String reportTypeCode,
        @NotBlank String formatCode
) {
}

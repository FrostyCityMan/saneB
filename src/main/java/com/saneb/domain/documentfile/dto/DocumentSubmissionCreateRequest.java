package com.saneb.domain.documentfile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DocumentSubmissionCreateRequest(
        @NotNull(message = "fileId is required")
        UUID fileId,

        @NotBlank(message = "resourceTypeCode is required")
        @Size(max = 40, message = "resourceTypeCode must be 40 characters or less")
        String resourceTypeCode,

        @NotNull(message = "resourceId is required")
        UUID resourceId,

        @NotBlank(message = "documentTypeCode is required")
        @Size(max = 80, message = "documentTypeCode must be 80 characters or less")
        String documentTypeCode
) {
}

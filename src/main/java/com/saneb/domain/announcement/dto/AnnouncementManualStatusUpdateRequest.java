package com.saneb.domain.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementManualStatusUpdateRequest(
        @NotBlank(message = "manualStatusCode is required")
        @Size(max = 30, message = "manualStatusCode must be 30 characters or less")
        String manualStatusCode,

        String reason
) {
}

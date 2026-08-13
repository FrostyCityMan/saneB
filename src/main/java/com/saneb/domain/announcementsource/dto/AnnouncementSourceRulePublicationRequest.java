package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** DRAFT release를 Golden QA 후 게시하는 요청입니다. */
public record AnnouncementSourceRulePublicationRequest(
        @NotNull(message = "expectedVersion is required")
        @PositiveOrZero(message = "expectedVersion must be 0 or greater")
        Integer expectedVersion,
        @NotBlank(message = "changeReason is required")
        @Size(max = 1000, message = "changeReason must be 1000 characters or less")
        String changeReason,
        @NotBlank(message = "goldenSetRunId is required")
        @Size(max = 100, message = "goldenSetRunId must be 100 characters or less")
        String goldenSetRunId
) {
}

package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** ACTIVE 규칙 버전을 복제해 DRAFT를 만드는 요청입니다. */
public record AnnouncementSourceRuleReleaseCreateRequest(
        @NotNull(message = "expectedVersion is required")
        @PositiveOrZero(message = "expectedVersion must be 0 or greater")
        Integer expectedVersion,
        @NotBlank(message = "changeReason is required")
        @Size(max = 1000, message = "changeReason must be 1000 characters or less")
        String changeReason
) {
}

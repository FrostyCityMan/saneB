package com.saneb.domain.matching.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchingCaseCreateRequest(
        @NotNull UUID announcementId,
        @NotNull UUID memberUserId,
        @NotNull UUID verificationId
) {
}

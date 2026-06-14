package com.saneb.domain.matching.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchingFinalRecalculateRequest(
        @NotNull(message = "회원 식별자를 선택하세요.")
        UUID memberUserId
) {
}

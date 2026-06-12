package com.saneb.domain.matching.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchingCandidateGenerateRequest(
        @NotNull(message = "회원 ID를 선택하세요.")
        UUID memberUserId
) {
}

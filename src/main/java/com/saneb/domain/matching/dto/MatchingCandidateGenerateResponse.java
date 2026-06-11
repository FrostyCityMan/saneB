package com.saneb.domain.matching.dto;

import java.util.List;
import java.util.UUID;

public record MatchingCandidateGenerateResponse(
        UUID memberUserId,
        int createdCount,
        int skippedCount,
        List<MatchingCaseSummaryResponse> candidates
) {
}

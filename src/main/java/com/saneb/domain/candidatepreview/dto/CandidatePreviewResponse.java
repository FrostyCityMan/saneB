package com.saneb.domain.candidatepreview.dto;

import java.math.BigDecimal;

public record CandidatePreviewResponse(
        long possibleCandidateCount,
        BigDecimal minSupportAmount,
        BigDecimal maxSupportAmount,
        String criteriaNotice
) {
}

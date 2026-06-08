package com.saneb.domain.candidatepreview.vo;

import java.math.BigDecimal;

public record CandidatePreviewRow(
        long possibleCandidateCount,
        BigDecimal minSupportAmount,
        BigDecimal maxSupportAmount
) {
}

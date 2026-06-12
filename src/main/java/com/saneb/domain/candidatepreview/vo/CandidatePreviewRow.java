package com.saneb.domain.candidatepreview.vo;

import java.math.BigDecimal;

public record CandidatePreviewRow(
        Long possibleCandidateCount,
        BigDecimal minSupportAmount,
        BigDecimal maxSupportAmount
) {
}

package com.saneb.domain.candidatepreview.vo;

import java.math.BigDecimal;

public record CandidatePreviewSearchCondition(
        String regionCode,
        BigDecimal annualRevenue,
        BigDecimal businessYears,
        String hasSpouseCode,
        String hasChildCode
) {
}

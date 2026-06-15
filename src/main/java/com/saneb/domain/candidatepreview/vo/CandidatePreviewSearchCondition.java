package com.saneb.domain.candidatepreview.vo;

import java.math.BigDecimal;

public record CandidatePreviewSearchCondition(
        String regionCode,
        BigDecimal annualRevenue,
        BigDecimal businessYears,
        BigDecimal age,
        String ksicCode,
        String hasSpouseCode,
        String hasChildCode,
        String hasParentCode
) {
}

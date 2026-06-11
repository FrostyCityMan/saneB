package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberProfileRow(
        UUID userId,
        Integer birthYear,
        String regionCode,
        Boolean hasIncome,
        String incomePresenceCode,
        BigDecimal incomeAmount,
        String healthInsuranceBasisCode
) {
}

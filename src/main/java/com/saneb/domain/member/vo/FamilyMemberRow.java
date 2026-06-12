package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record FamilyMemberRow(
        UUID familyMemberId,
        String relationTypeCode,
        Integer birthYear,
        Boolean hasIncome,
        String incomePresenceCode,
        BigDecimal incomeAmount
) {
}

package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record FamilyMemberCommand(
        UUID familyMemberId,
        UUID userId,
        String relationTypeCode,
        Integer birthYear,
        Boolean hasIncome,
        String incomePresenceCode,
        BigDecimal incomeAmount,
        UUID actorUserId
) {
}

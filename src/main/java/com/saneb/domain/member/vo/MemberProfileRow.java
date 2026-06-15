package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberProfileRow(
        UUID userId,
        Integer birthYear,
        String regionCode,
        String postalCode,
        String roadAddress,
        String jibunAddress,
        String detailAddress,
        String sidoName,
        String sigunguName,
        String eupmyeondongName,
        String legalDongCode,
        String roadNameCode,
        String buildingManagementNo,
        String addressSourceCode,
        Boolean hasIncome,
        String incomePresenceCode,
        BigDecimal incomeAmount,
        String healthInsuranceBasisCode
) {
}

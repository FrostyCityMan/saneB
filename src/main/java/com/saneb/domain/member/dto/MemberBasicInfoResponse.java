package com.saneb.domain.member.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MemberBasicInfoResponse(
        UUID userId,
        Integer birthYear,
        String regionCode,
        Boolean hasIncome,
        String incomePresenceCode,
        BigDecimal incomeAmount,
        String healthInsuranceBasisCode,
        BusinessInfoResponse business,
        List<FamilyInfoResponse> families
) {

    public record BusinessInfoResponse(
            String businessRegistrationNo,
            String businessName,
            String workplaceRegionCode,
            LocalDate openingDate,
            String ksicCode,
            String businessTypeCode,
            String companyStageCode,
            BigDecimal annualRevenue,
            Integer annualRevenueYear,
            Boolean hasPolicyFundUsage,
            Boolean hasGuaranteeUsage
    ) {
    }

    public record FamilyInfoResponse(
            UUID familyMemberId,
            String relationTypeCode,
            Integer birthYear,
            Boolean hasIncome,
            String incomePresenceCode,
            BigDecimal incomeAmount
    ) {
    }
}

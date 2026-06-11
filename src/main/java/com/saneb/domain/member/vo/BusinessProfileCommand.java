package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessProfileCommand(
        UUID businessProfileId,
        UUID userId,
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
        Boolean hasGuaranteeUsage,
        UUID actorUserId
) {
}

package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessProfileRow(
        UUID businessProfileId,
        String businessRegistrationNo,
        String businessName,
        String workplaceRegionCode,
        String workplacePostalCode,
        String workplaceRoadAddress,
        String workplaceJibunAddress,
        String workplaceDetailAddress,
        String workplaceSidoName,
        String workplaceSigunguName,
        String workplaceEupmyeondongName,
        String workplaceLegalDongCode,
        String workplaceRoadNameCode,
        String workplaceBuildingManagementNo,
        String workplaceAddressSourceCode,
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

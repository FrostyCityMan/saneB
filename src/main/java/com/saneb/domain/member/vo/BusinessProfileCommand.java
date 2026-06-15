package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessProfileCommand(
        UUID businessProfileId,
        UUID userId,
        String representativeName,
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
        Integer employeeCount,
        Integer regularEmployeeCount,
        Integer plannedHireCount,
        Integer niceCreditScore,
        Integer kcbCreditScore,
        Boolean hasExistingLoan,
        Boolean hasPolicyFundUsage,
        Boolean hasGuaranteeUsage,
        UUID actorUserId
) {
}

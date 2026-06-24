/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: BusinessProfileRow.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessProfileRow(
        UUID businessProfileId,
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
        Boolean hasGuaranteeUsage
) {
}

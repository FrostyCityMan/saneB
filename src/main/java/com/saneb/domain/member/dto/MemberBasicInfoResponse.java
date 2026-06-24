/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberBasicInfoResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MemberBasicInfoResponse(
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
        String healthInsuranceBasisCode,
        BusinessInfoResponse business,
        List<FamilyInfoResponse> families,
        List<InterviewResponse> interviewResponses,
        List<DocumentInputResponse> documentInputs
) {

    public record BusinessInfoResponse(
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

    public record FamilyInfoResponse(
            UUID familyMemberId,
            String relationTypeCode,
            Integer birthYear,
            String schoolAgeStatusCode,
            String enrollmentStatusCode,
            Boolean cohabiting,
            Boolean supported,
            Boolean hasIncome,
            String incomePresenceCode,
            BigDecimal incomeAmount
    ) {
    }

    public record InterviewResponse(
            String questionCode,
            String questionLabel,
            String answerCode,
            String answerLabel,
            String note
    ) {
    }

    public record DocumentInputResponse(
            String documentTypeCode,
            String documentTypeLabel,
            boolean selected,
            List<DocumentFieldInputResponse> fields
    ) {
    }

    public record DocumentFieldInputResponse(
            UUID standardFieldId,
            String fieldKey,
            String fieldLabel,
            String fieldTypeCode,
            String scopeCode,
            boolean required,
            int sortOrder,
            String helpText,
            String valueText,
            BigDecimal valueNumber,
            LocalDate valueDate,
            Boolean valueBoolean
    ) {
    }
}

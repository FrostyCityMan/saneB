package com.saneb.domain.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MemberBasicInfoSaveRequest(
        Integer birthYear,
        @Size(max = 30, message = "지역 코드는 30자 이하로 입력하세요.")
        String regionCode,
        Boolean hasIncome,
        @Size(max = 30, message = "소득 여부 코드는 30자 이하로 입력하세요.")
        String incomePresenceCode,
        @DecimalMin(value = "0", message = "소득 금액은 0 이상으로 입력하세요.")
        BigDecimal incomeAmount,
        @Size(max = 50, message = "건강보험 자격 코드는 50자 이하로 입력하세요.")
        String healthInsuranceBasisCode,
        @Valid
        BusinessInfoRequest business,
        @Valid
        List<FamilyInfoRequest> families
) {

    public record BusinessInfoRequest(
            @Size(max = 30, message = "사업자등록번호는 30자 이하로 입력하세요.")
            String businessRegistrationNo,
            @Size(max = 200, message = "상호명은 200자 이하로 입력하세요.")
            String businessName,
            @Size(max = 30, message = "사업장 지역 코드는 30자 이하로 입력하세요.")
            String workplaceRegionCode,
            LocalDate openingDate,
            @Size(max = 30, message = "업종 코드는 30자 이하로 입력하세요.")
            String ksicCode,
            @Size(max = 50, message = "사업자 유형 코드는 50자 이하로 입력하세요.")
            String businessTypeCode,
            @Size(max = 50, message = "사업 상태 코드는 50자 이하로 입력하세요.")
            String companyStageCode,
            @DecimalMin(value = "0", message = "연매출은 0 이상으로 입력하세요.")
            BigDecimal annualRevenue,
            Integer annualRevenueYear,
            Boolean hasPolicyFundUsage,
            Boolean hasGuaranteeUsage
    ) {
    }

    public record FamilyInfoRequest(
            @NotBlank(message = "가족 관계를 선택하세요.")
            @Size(max = 30, message = "가족 관계 코드는 30자 이하로 입력하세요.")
            String relationTypeCode,
            Integer birthYear,
            Boolean hasIncome,
            @Size(max = 30, message = "가족 소득 여부 코드는 30자 이하로 입력하세요.")
            String incomePresenceCode,
            @DecimalMin(value = "0", message = "가족 소득 금액은 0 이상으로 입력하세요.")
            BigDecimal incomeAmount
    ) {
    }
}

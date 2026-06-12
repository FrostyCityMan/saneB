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
        List<FamilyInfoResponse> families,
        List<DocumentInputResponse> documentInputs
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

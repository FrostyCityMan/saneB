package com.saneb.domain.partnerverification.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PartnerVerificationDetailsResponse(
        UUID verificationId,
        UUID memberUserId,
        UUID partnerUserId,
        UUID businessProfileId,
        String verificationCode,
        String memberUserCode,
        String partnerUserCode,
        String statusCode,
        boolean current,
        boolean matchingBlocked,
        OffsetDateTime submittedAt,
        OffsetDateTime verifiedAt,
        UUID reviewedBy,
        String reviewNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        MemberValuesResponse memberValues,
        BusinessValuesResponse businessValues,
        List<FamilyValueResponse> familyValues,
        List<RestrictionFlagResponse> restrictionFlags,
        List<DocumentResponse> documents
) {

    public record MemberValuesResponse(
            Integer birthYear,
            String address,
            String regionCode,
            Boolean householder,
            Boolean householdMember,
            String healthInsuranceBasisCode,
            Boolean hasIncome
    ) {
    }

    public record BusinessValuesResponse(
            BigDecimal annualRevenue,
            Integer employeeCount,
            Integer regularEmployeeCount,
            String taxStatusCode,
            Integer niceCreditScore,
            Integer kcbCreditScore,
            Boolean hasExistingLoan,
            Boolean hasPolicyFundUsage,
            Boolean hasGuaranteeUsage,
            LocalDate financialCheckedOn
    ) {
    }

    public record FamilyValueResponse(
            String relationTypeCode,
            Integer birthYear,
            String address,
            String schoolAgeStatusCode,
            String enrollmentStatusCode,
            Boolean cohabiting,
            Boolean supported,
            Boolean hasIncome
    ) {
    }

    public record RestrictionFlagResponse(
            String restrictionCode,
            boolean checked,
            String note
    ) {
    }

    public record DocumentResponse(
            String documentTypeCode,
            String sourceTypeCode,
            boolean checked,
            UUID checkedBy,
            OffsetDateTime checkedAt,
            String note
    ) {
    }
}

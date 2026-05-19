package com.saneb.domain.announcement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AnnouncementDetailsResponse(
        UUID announcementId,
        String targetTypeCode,
        String title,
        String agencyName,
        String summary,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String manualStatusCode,
        String approvalStatusCode,
        String incomeJudgementCode,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OptionResponse> options,
        ConditionsResponse conditions,
        List<ProgressStepResponse> steps
) {

    public record OptionResponse(
            String optionGroupCode,
            String optionCode
    ) {
    }

    public record ConditionsResponse(
            List<IndustryConditionResponse> industryConditions,
            List<NumericConditionResponse> numericConditions,
            List<OptionConditionResponse> optionConditions,
            List<DocumentRequirementResponse> documentRequirements
    ) {
    }

    public record IndustryConditionResponse(
            String conditionTypeCode,
            String ksicCode
    ) {
    }

    public record NumericConditionResponse(
            String conditionScopeCode,
            String conditionKey,
            String comparatorCode,
            BigDecimal valueNumber,
            BigDecimal minNumber,
            BigDecimal maxNumber,
            String unitCode
    ) {
    }

    public record OptionConditionResponse(
            String conditionScopeCode,
            String conditionKey,
            String optionCode,
            String optionText
    ) {
    }

    public record DocumentRequirementResponse(
            String documentTypeCode,
            Boolean required,
            Integer sortOrder
    ) {
    }

    public record ProgressStepResponse(
            UUID stepId,
            Integer stepOrder,
            String stepName,
            String guideMessage,
            String actionGuide,
            String completionConditionCode,
            String nextConditionCode,
            Boolean active,
            List<StepButtonResponse> buttons,
            List<StepDocumentResponse> documents
    ) {
    }

    public record StepButtonResponse(
            String buttonCode,
            String buttonLabel,
            String buttonActionCode,
            UUID nextStepId,
            Integer sortOrder
    ) {
    }

    public record StepDocumentResponse(
            String documentTypeCode,
            Boolean required,
            Integer sortOrder
    ) {
    }
}

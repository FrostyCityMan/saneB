package com.saneb.domain.announcement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AnnouncementConditionsSaveRequest(
        @Valid
        List<IndustryConditionRequest> industryConditions,

        @Valid
        List<NumericConditionRequest> numericConditions,

        @Valid
        List<OptionConditionRequest> optionConditions,

        @Valid
        List<DocumentRequirementRequest> documentRequirements
) {

    public record IndustryConditionRequest(
            @NotBlank(message = "conditionTypeCode is required")
            @Size(max = 30, message = "conditionTypeCode must be 30 characters or less")
            String conditionTypeCode,

            @NotBlank(message = "ksicCode is required")
            @Size(max = 30, message = "ksicCode must be 30 characters or less")
            String ksicCode
    ) {
    }

    public record NumericConditionRequest(
            @NotBlank(message = "conditionScopeCode is required")
            @Size(max = 30, message = "conditionScopeCode must be 30 characters or less")
            String conditionScopeCode,

            @NotBlank(message = "conditionKey is required")
            @Size(max = 80, message = "conditionKey must be 80 characters or less")
            String conditionKey,

            @NotBlank(message = "comparatorCode is required")
            @Size(max = 30, message = "comparatorCode must be 30 characters or less")
            String comparatorCode,

            @DecimalMin(value = "0", inclusive = false, message = "valueNumber must be greater than 0")
            BigDecimal valueNumber,

            @DecimalMin(value = "0", inclusive = false, message = "minNumber must be greater than 0")
            BigDecimal minNumber,

            @DecimalMin(value = "0", inclusive = false, message = "maxNumber must be greater than 0")
            BigDecimal maxNumber,

            @Size(max = 30, message = "unitCode must be 30 characters or less")
            String unitCode,

            UUID standardFieldId
    ) {
    }

    public record OptionConditionRequest(
            @NotBlank(message = "conditionScopeCode is required")
            @Size(max = 30, message = "conditionScopeCode must be 30 characters or less")
            String conditionScopeCode,

            @NotBlank(message = "conditionKey is required")
            @Size(max = 80, message = "conditionKey must be 80 characters or less")
            String conditionKey,

            @NotBlank(message = "optionCode is required")
            @Size(max = 80, message = "optionCode must be 80 characters or less")
            String optionCode,

            @Size(max = 500, message = "optionText must be 500 characters or less")
            String optionText,

            UUID standardFieldId
    ) {
    }

    public record DocumentRequirementRequest(
            @NotBlank(message = "documentTypeCode is required")
            @Size(max = 80, message = "documentTypeCode must be 80 characters or less")
            String documentTypeCode,

            @NotNull(message = "required is required")
            Boolean required,

            @NotNull(message = "sortOrder is required")
            Integer sortOrder,

            UUID standardFieldId
    ) {
    }
}

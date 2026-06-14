package com.saneb.domain.dynamicinput.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AnnouncementInputRequirementsSaveRequest(
        @Valid List<RequirementRequest> requirements
) {

    public record RequirementRequest(
            @NotBlank String fieldKey,
            @NotBlank String fieldLabel,
            @NotBlank String fieldTypeCode,
            @NotBlank String scopeCode,
            @NotNull Boolean required,
            @NotNull Boolean sensitive,
            @Min(0) int sortOrder,
            UUID standardFieldId,
            String helpText,
            @Valid List<OptionRequest> options
    ) {
    }

    public record OptionRequest(
            @NotBlank String optionCode,
            @NotBlank String optionLabel,
            @Min(0) int sortOrder
    ) {
    }
}

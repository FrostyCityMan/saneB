package com.saneb.domain.dynamicinput.dto;

import java.util.List;
import java.util.UUID;

public record AnnouncementInputRequirementsResponse(
        UUID announcementId,
        List<RequirementResponse> requirements
) {

    public record RequirementResponse(
            UUID requirementId,
            String fieldKey,
            String fieldLabel,
            String fieldTypeCode,
            String scopeCode,
            boolean required,
            boolean sensitive,
            int sortOrder,
            UUID standardFieldId,
            String helpText,
            List<OptionResponse> options
    ) {
    }

    public record OptionResponse(
            UUID optionId,
            String optionCode,
            String optionLabel,
            int sortOrder
    ) {
    }
}

package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementOptionConditionRow(
        String conditionScopeCode,
        String conditionKey,
        String optionCode,
        String optionText,
        UUID standardFieldId
) {
}

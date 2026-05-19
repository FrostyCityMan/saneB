package com.saneb.domain.announcement.vo;

public record AnnouncementOptionConditionRow(
        String conditionScopeCode,
        String conditionKey,
        String optionCode,
        String optionText
) {
}

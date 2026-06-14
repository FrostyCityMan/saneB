package com.saneb.domain.announcement.vo;

import java.math.BigDecimal;
import java.util.UUID;

public record AnnouncementNumericConditionRow(
        String conditionScopeCode,
        String conditionKey,
        String comparatorCode,
        BigDecimal valueNumber,
        BigDecimal minNumber,
        BigDecimal maxNumber,
        String unitCode,
        UUID standardFieldId
) {
}

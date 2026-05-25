package com.saneb.domain.dynamicinput.vo;

import java.util.UUID;

public record AnnouncementInputOptionRow(
        UUID optionId,
        UUID requirementId,
        String optionCode,
        String optionLabel,
        Integer sortOrder
) {
}

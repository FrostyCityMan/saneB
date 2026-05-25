package com.saneb.domain.dynamicinput.vo;

import java.util.UUID;

public record AnnouncementInputOptionCommand(
        UUID optionId,
        UUID requirementId,
        String optionCode,
        String optionLabel,
        int sortOrder,
        UUID actorUserId
) {
}

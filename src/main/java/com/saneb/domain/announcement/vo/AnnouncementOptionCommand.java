package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementOptionCommand(
        UUID announcementId,
        String optionGroupCode,
        String optionCode,
        UUID actorUserId
) {
}

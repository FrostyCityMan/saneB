package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

/** ACTIVE release를 복제할 신규 DRAFT 식별값입니다. */
public record AnnouncementSourceRuleReleaseInsertCommand(
        UUID releaseId,
        UUID sourceReleaseId,
        String releaseCode,
        int versionNo,
        UUID actorUserId,
        String changeReason
) {
}

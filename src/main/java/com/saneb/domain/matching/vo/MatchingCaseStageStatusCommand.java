package com.saneb.domain.matching.vo;

import java.util.List;
import java.util.UUID;

public record MatchingCaseStageStatusCommand(
        UUID memberUserId,
        String matchingStageCode,
        String statusCode,
        UUID actorUserId,
        List<UUID> eligibleAnnouncementIds
) {
}

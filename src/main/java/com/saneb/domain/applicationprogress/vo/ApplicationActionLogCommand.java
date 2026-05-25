package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record ApplicationActionLogCommand(
        UUID progressId,
        UUID stepId,
        UUID actorUserId,
        String actionCode,
        String buttonCode,
        String inputJson
) {
}

package com.saneb.domain.applicationprogress.vo;

import java.util.UUID;

public record ApplicationChecklistSaveCommand(
        UUID progressId,
        UUID stepDocumentId,
        Boolean checked,
        UUID actorUserId
) {
}

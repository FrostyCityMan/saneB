package com.saneb.domain.applicationprogress.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationChecklistRow(
        UUID checklistId,
        UUID progressId,
        UUID stepDocumentId,
        UUID stepId,
        String documentTypeCode,
        Boolean required,
        Boolean checked,
        OffsetDateTime checkedAt,
        UUID checkedBy
) {
}

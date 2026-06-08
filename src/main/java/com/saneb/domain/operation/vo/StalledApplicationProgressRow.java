package com.saneb.domain.operation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StalledApplicationProgressRow(
        UUID progressId,
        UUID memberUserId,
        UUID currentStepId,
        String statusCode,
        OffsetDateTime updatedAt
) {
}

package com.saneb.domain.aiassist.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiAssistRow(
        UUID requestId,
        UUID resultId,
        String assistTypeCode,
        String resourceType,
        UUID resourceId,
        String requestStatusCode,
        String providerCode,
        String modelCode,
        String reviewStatusCode,
        String resultText,
        UUID requestedBy,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}

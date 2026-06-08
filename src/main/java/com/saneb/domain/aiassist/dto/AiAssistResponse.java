package com.saneb.domain.aiassist.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiAssistResponse(
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

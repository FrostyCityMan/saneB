package com.saneb.domain.aiassist.vo;

import java.util.UUID;

public record AiAssistInsertCommand(
        UUID requestId,
        String assistTypeCode,
        String resourceType,
        UUID resourceId,
        String inputHashSha256,
        int inputLength,
        UUID requestedBy,
        String statusCode,
        String providerCode,
        String modelCode
) {
}

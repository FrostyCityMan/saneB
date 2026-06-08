package com.saneb.domain.aiassist.vo;

import java.util.UUID;

public record AiAssistResultInsertCommand(
        UUID resultId,
        UUID requestId,
        String resultText,
        String reviewStatusCode,
        int promptTokenCount,
        int completionTokenCount,
        int latencyMs,
        String metadataJson
) {
}

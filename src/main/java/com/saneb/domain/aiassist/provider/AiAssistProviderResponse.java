package com.saneb.domain.aiassist.provider;

public record AiAssistProviderResponse(
        String providerCode,
        String modelCode,
        String resultText,
        int promptTokenCount,
        int completionTokenCount,
        String metadataJson
) {
}

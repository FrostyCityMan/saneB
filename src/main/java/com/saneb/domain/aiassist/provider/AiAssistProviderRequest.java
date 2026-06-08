package com.saneb.domain.aiassist.provider;

public record AiAssistProviderRequest(
        String assistTypeCode,
        String resourceType,
        int inputLength,
        boolean hasOperatorNote
) {
}

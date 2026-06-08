package com.saneb.domain.aiassist.vo;

public record AiAssistSearchCondition(
        String assistTypeCode,
        String resourceType,
        String reviewStatusCode,
        int limit,
        int offset
) {
}

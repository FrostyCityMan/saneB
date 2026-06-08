package com.saneb.domain.aiassist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiAssistCreateRequest(
        @NotBlank String assistTypeCode,
        @Size(max = 50) String resourceType,
        UUID resourceId,
        @NotBlank @Size(max = 2000) String inputText,
        @Size(max = 500) String operatorNote
) {
}

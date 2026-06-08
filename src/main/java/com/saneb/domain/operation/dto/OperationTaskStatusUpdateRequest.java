package com.saneb.domain.operation.dto;

import jakarta.validation.constraints.NotBlank;

public record OperationTaskStatusUpdateRequest(
        @NotBlank String statusCode
) {
}

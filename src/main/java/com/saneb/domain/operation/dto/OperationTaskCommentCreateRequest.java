package com.saneb.domain.operation.dto;

import jakarta.validation.constraints.NotBlank;

public record OperationTaskCommentCreateRequest(
        @NotBlank String commentText
) {
}

package com.saneb.domain.operation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationTaskCommentResponse(
        UUID commentId,
        UUID taskId,
        UUID authorUserId,
        String commentText,
        OffsetDateTime createdAt
) {
}

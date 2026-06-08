package com.saneb.domain.operation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationTaskCommentRow(
        UUID commentId,
        UUID taskId,
        UUID authorUserId,
        String commentText,
        OffsetDateTime createdAt
) {
}

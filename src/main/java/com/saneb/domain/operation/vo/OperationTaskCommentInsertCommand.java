package com.saneb.domain.operation.vo;

import java.util.UUID;

public record OperationTaskCommentInsertCommand(
        UUID commentId,
        UUID taskId,
        UUID authorUserId,
        String commentText
) {
}

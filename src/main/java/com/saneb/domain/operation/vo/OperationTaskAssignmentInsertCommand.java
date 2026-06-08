package com.saneb.domain.operation.vo;

import java.util.UUID;

public record OperationTaskAssignmentInsertCommand(
        UUID assignmentId,
        UUID taskId,
        UUID assigneeUserId,
        UUID assignedBy
) {
}

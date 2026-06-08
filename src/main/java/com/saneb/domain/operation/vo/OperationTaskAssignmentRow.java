package com.saneb.domain.operation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationTaskAssignmentRow(
        UUID assignmentId,
        UUID taskId,
        UUID assigneeUserId,
        String statusCode,
        UUID assignedBy,
        OffsetDateTime assignedAt,
        OffsetDateTime completedAt
) {
}

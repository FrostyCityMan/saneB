package com.saneb.domain.operation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationTaskAssignmentResponse(
        UUID assignmentId,
        UUID taskId,
        UUID assigneeUserId,
        String statusCode,
        UUID assignedBy,
        OffsetDateTime assignedAt,
        OffsetDateTime completedAt
) {
}

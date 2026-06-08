package com.saneb.domain.operation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationTaskResponse(
        UUID taskId,
        String taskTypeCode,
        String statusCode,
        String priorityCode,
        String title,
        String description,
        String resourceType,
        UUID resourceId,
        OffsetDateTime dueAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

package com.saneb.domain.operation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationTaskRow(
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

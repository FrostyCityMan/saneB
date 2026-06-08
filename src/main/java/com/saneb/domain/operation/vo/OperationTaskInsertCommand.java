package com.saneb.domain.operation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperationTaskInsertCommand(
        UUID taskId,
        String taskTypeCode,
        String priorityCode,
        String title,
        String description,
        String resourceType,
        UUID resourceId,
        OffsetDateTime dueAt,
        UUID actorUserId
) {
}

package com.saneb.domain.operation.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OperationTaskCreateRequest(
        @NotBlank String taskTypeCode,
        String priorityCode,
        @NotBlank String title,
        String description,
        String resourceType,
        UUID resourceId,
        OffsetDateTime dueAt,
        List<UUID> assigneeUserIds
) {
}

package com.saneb.domain.operation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OperationTaskAssignmentCreateRequest(
        @NotNull UUID assigneeUserId
) {
}

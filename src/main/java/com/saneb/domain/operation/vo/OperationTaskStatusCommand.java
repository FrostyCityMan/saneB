package com.saneb.domain.operation.vo;

import java.util.UUID;

public record OperationTaskStatusCommand(
        UUID taskId,
        String statusCode,
        UUID actorUserId
) {
}

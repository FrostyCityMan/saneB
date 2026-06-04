package com.saneb.domain.adminuser.vo;

import java.util.UUID;

public record AdminUserStatusCommand(
        UUID userId,
        String statusCode,
        UUID actorUserId
) {
}

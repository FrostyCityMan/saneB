package com.saneb.domain.adminuser.vo;

import java.util.UUID;

public record AdminUserRoleCommand(
        UUID userId,
        String roleCode,
        UUID actorUserId
) {
}

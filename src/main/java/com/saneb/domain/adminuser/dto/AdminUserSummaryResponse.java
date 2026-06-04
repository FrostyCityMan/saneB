package com.saneb.domain.adminuser.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID userId,
        String loginId,
        String name,
        String phone,
        String email,
        String statusCode,
        boolean passwordResetRequired,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> roles
) {
}

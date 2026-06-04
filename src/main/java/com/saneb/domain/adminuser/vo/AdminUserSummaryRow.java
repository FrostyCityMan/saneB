package com.saneb.domain.adminuser.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserSummaryRow(
        UUID userId,
        String loginId,
        String name,
        String phone,
        String email,
        String statusCode,
        Boolean passwordResetRequired,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String roleCodesText
) {
}

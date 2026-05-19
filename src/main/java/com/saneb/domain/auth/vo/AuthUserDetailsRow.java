package com.saneb.domain.auth.vo;

import java.util.UUID;

public record AuthUserDetailsRow(
        UUID userId,
        String loginId,
        String passwordHash,
        String name,
        String statusCode,
        Boolean passwordResetRequired,
        UUID memberProfileId,
        UUID businessProfileId,
        UUID partnerProfileId
) {
}

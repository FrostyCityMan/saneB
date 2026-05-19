package com.saneb.domain.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthMeResponse(
        UUID userId,
        String loginId,
        String name,
        List<String> roles,
        String primaryRole,
        String defaultRoute,
        boolean passwordResetRequired,
        ProfileResponse profile
) {

    public record ProfileResponse(
            UUID memberProfileId,
            UUID businessProfileId,
            UUID partnerProfileId
    ) {
    }
}

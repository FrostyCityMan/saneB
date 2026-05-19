package com.saneb.domain.auth.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String loginId,
        String name,
        List<String> roles,
        String primaryRole,
        String defaultRoute,
        boolean passwordResetRequired
) {

    public static LoginResponse from(AuthMeResponse authMe) {
        return new LoginResponse(
                authMe.userId(),
                authMe.loginId(),
                authMe.name(),
                authMe.roles(),
                authMe.primaryRole(),
                authMe.defaultRoute(),
                authMe.passwordResetRequired()
        );
    }
}

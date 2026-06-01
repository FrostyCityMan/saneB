package com.saneb.domain.auth.vo;

public record AuthSignupCommand(
        String loginId,
        String passwordHash,
        String name,
        String phone,
        String email
) {
}

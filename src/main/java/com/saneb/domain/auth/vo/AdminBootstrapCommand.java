package com.saneb.domain.auth.vo;

import java.util.UUID;

public record AdminBootstrapCommand(
        UUID userId,
        String loginId,
        String passwordHash,
        String name
) {
}

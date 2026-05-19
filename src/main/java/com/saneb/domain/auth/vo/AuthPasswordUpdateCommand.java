package com.saneb.domain.auth.vo;

import java.util.UUID;

public record AuthPasswordUpdateCommand(
        UUID userId,
        String passwordHash
) {
}

package com.saneb.domain.auth.vo;

import java.util.UUID;

public record AuthLoginHistoryCommand(
        UUID userId,
        String loginId,
        String loginResultCode,
        String ipAddress,
        String userAgent,
        String failureReasonCode
) {
}

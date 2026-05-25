package com.saneb.domain.matching.vo;

import java.util.UUID;

public record VerificationMatchingRow(
        UUID verificationId,
        UUID memberUserId,
        String statusCode,
        Boolean current,
        Boolean matchingBlocked
) {
}

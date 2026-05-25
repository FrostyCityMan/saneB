package com.saneb.domain.dashboard.vo;

import java.time.OffsetDateTime;

public record DashboardVerificationStatusRow(
        String statusCode,
        OffsetDateTime verifiedAt
) {
}

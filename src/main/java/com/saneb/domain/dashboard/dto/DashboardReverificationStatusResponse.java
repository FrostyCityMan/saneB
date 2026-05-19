package com.saneb.domain.dashboard.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record DashboardReverificationStatusResponse(
        boolean required,
        OffsetDateTime lastVerifiedAt,
        String reasonCode,
        List<String> requiredItems
) {
}

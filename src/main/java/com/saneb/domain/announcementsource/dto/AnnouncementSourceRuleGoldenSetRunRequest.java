package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AnnouncementSourceRuleGoldenSetRunRequest(
        @NotNull @PositiveOrZero Integer expectedVersion
) {
}

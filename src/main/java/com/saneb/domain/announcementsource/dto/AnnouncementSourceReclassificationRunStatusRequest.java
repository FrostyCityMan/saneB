package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AnnouncementSourceReclassificationRunStatusRequest(
        @NotNull @PositiveOrZero Integer expectedVersion,
        @NotBlank @Size(max = 1000) String changeReason
) {
}

package com.saneb.domain.dashboard.dto;

import java.time.LocalDate;

public record DashboardCurrentActionResponse(
        String actionCode,
        String title,
        String description,
        String primaryButtonLabel,
        String route,
        LocalDate dueDate,
        int displayOrder
) {
}

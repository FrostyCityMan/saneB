package com.saneb.domain.dashboard.vo;

import java.time.LocalDate;
import java.util.UUID;

public record DashboardCurrentStepRow(
        UUID progressId,
        String stepStatusCode,
        String stepName,
        String guideMessage,
        String actionGuide,
        String buttonLabel,
        LocalDate dueDate
) {
}

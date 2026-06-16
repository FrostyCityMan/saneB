package com.saneb.domain.announcement.service.impl;

import java.time.LocalDate;
import java.util.Map;

final class AnnouncementStatusPolicy {

    private static final String NORMAL = "NORMAL";

    private static final Map<String, String> AUTOMATIC_STATUS_LABELS = Map.of(
            "UPCOMING", "모집예정",
            "OPEN", "접수중",
            "CLOSING_SOON", "마감임박",
            "ENDED", "종료"
    );

    private static final Map<String, String> MANUAL_STATUS_LABELS = Map.of(
            NORMAL, "정상 노출",
            "PAUSED", "일시중지",
            "EARLY_CLOSED", "조기마감",
            "SUSPENDED", "접수중단",
            "BUDGET_EXHAUSTED", "예산소진",
            "CLOSED", "종료",
            "HIDDEN", "숨김처리"
    );

    private AnnouncementStatusPolicy() {
    }

    static AnnouncementStatusView selectStatus(
            LocalDate applicationStartDate,
            LocalDate applicationEndDate,
            String manualStatusCode,
            LocalDate today
    ) {
        String automaticStatusCode = selectAutomaticStatusCode(applicationStartDate, applicationEndDate, today);
        String normalizedManualStatusCode = manualStatusCode == null || manualStatusCode.isBlank()
                ? NORMAL
                : manualStatusCode;
        String effectiveStatusCode = NORMAL.equals(normalizedManualStatusCode)
                ? automaticStatusCode
                : normalizedManualStatusCode;
        String effectiveStatusLabel = NORMAL.equals(normalizedManualStatusCode)
                ? automaticStatusLabel(automaticStatusCode)
                : manualStatusLabel(normalizedManualStatusCode);
        return new AnnouncementStatusView(
                automaticStatusCode,
                automaticStatusLabel(automaticStatusCode),
                effectiveStatusCode,
                effectiveStatusLabel
        );
    }

    private static String selectAutomaticStatusCode(
            LocalDate applicationStartDate,
            LocalDate applicationEndDate,
            LocalDate today
    ) {
        if (applicationStartDate != null && today.isBefore(applicationStartDate)) {
            return "UPCOMING";
        }
        if (applicationEndDate != null && today.isAfter(applicationEndDate)) {
            return "ENDED";
        }
        if (applicationEndDate != null && !applicationEndDate.isAfter(today.plusDays(3))) {
            return "CLOSING_SOON";
        }
        return "OPEN";
    }

    private static String automaticStatusLabel(String statusCode) {
        return AUTOMATIC_STATUS_LABELS.getOrDefault(statusCode, statusCode);
    }

    private static String manualStatusLabel(String statusCode) {
        return MANUAL_STATUS_LABELS.getOrDefault(statusCode, statusCode);
    }

    record AnnouncementStatusView(
            String automaticStatusCode,
            String automaticStatusLabel,
            String effectiveStatusCode,
            String effectiveStatusLabel
    ) {
    }
}

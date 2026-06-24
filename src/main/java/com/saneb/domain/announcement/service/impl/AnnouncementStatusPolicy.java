/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementStatusPolicy.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 객체를 생성합니다.
     */
    private AnnouncementStatusPolicy() {
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param applicationStartDate 입력 값
     *
     * @param applicationEndDate 입력 값
     *
     * @param manualStatusCode 입력 값
     *
     * @param today 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param applicationStartDate 입력 값
     *
     * @param applicationEndDate 입력 값
     *
     * @param today 입력 값
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
    private static String automaticStatusLabel(String statusCode) {
        return AUTOMATIC_STATUS_LABELS.getOrDefault(statusCode, statusCode);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param statusCode 입력 값
     *
     * @return 처리 결과
     */
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

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementStatusPolicyTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AnnouncementStatusPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 17);

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectStatusReturnsUpcomingBeforeStartDate() {
        AnnouncementStatusPolicy.AnnouncementStatusView status = AnnouncementStatusPolicy.selectStatus(
                TODAY.plusDays(1),
                TODAY.plusDays(10),
                "NORMAL",
                TODAY
        );

        assertThat(status.automaticStatusCode()).isEqualTo("UPCOMING");
        assertThat(status.automaticStatusLabel()).isEqualTo("모집예정");
        assertThat(status.effectiveStatusCode()).isEqualTo("UPCOMING");
        assertThat(status.effectiveStatusLabel()).isEqualTo("모집예정");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectStatusReturnsOpenWhenPeriodIsActiveAndEndDateIsNotClose() {
        AnnouncementStatusPolicy.AnnouncementStatusView status = AnnouncementStatusPolicy.selectStatus(
                TODAY.minusDays(1),
                TODAY.plusDays(10),
                "NORMAL",
                TODAY
        );

        assertThat(status.automaticStatusCode()).isEqualTo("OPEN");
        assertThat(status.automaticStatusLabel()).isEqualTo("접수중");
        assertThat(status.effectiveStatusCode()).isEqualTo("OPEN");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectStatusReturnsClosingSoonWithinThreeDaysToEndDate() {
        AnnouncementStatusPolicy.AnnouncementStatusView status = AnnouncementStatusPolicy.selectStatus(
                TODAY.minusDays(1),
                TODAY.plusDays(3),
                "NORMAL",
                TODAY
        );

        assertThat(status.automaticStatusCode()).isEqualTo("CLOSING_SOON");
        assertThat(status.automaticStatusLabel()).isEqualTo("마감임박");
        assertThat(status.effectiveStatusCode()).isEqualTo("CLOSING_SOON");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectStatusReturnsEndedAfterEndDate() {
        AnnouncementStatusPolicy.AnnouncementStatusView status = AnnouncementStatusPolicy.selectStatus(
                TODAY.minusDays(10),
                TODAY.minusDays(1),
                "NORMAL",
                TODAY
        );

        assertThat(status.automaticStatusCode()).isEqualTo("ENDED");
        assertThat(status.automaticStatusLabel()).isEqualTo("종료");
        assertThat(status.effectiveStatusCode()).isEqualTo("ENDED");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectStatusUsesManualStatusWhenManualStatusIsNotNormal() {
        AnnouncementStatusPolicy.AnnouncementStatusView status = AnnouncementStatusPolicy.selectStatus(
                TODAY.minusDays(1),
                TODAY.plusDays(10),
                "BUDGET_EXHAUSTED",
                TODAY
        );

        assertThat(status.automaticStatusCode()).isEqualTo("OPEN");
        assertThat(status.effectiveStatusCode()).isEqualTo("BUDGET_EXHAUSTED");
        assertThat(status.effectiveStatusLabel()).isEqualTo("예산소진");
    }
}

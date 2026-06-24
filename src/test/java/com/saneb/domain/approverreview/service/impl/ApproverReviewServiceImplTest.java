/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApproverReviewServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.approverreview.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.saneb.domain.approverreview.dao.ApproverReviewDao;
import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse;
import com.saneb.domain.approverreview.vo.ApproverAnnouncementReviewRow;
import com.saneb.domain.approverreview.vo.ApproverMatchingReviewRow;
import com.saneb.domain.approverreview.vo.ApproverProgressReviewRow;
import com.saneb.domain.approverreview.vo.ApproverVerificationReviewRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApproverReviewServiceImplTest {

    @Mock
    private ApproverReviewDao approverReviewDao;

    private ApproverReviewServiceImpl approverReviewService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        approverReviewService = new ApproverReviewServiceImpl(approverReviewDao);
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectSummaryMapsApproverReviewCountsWithoutSensitiveValues() {
        when(approverReviewDao.selectAnnouncementReview()).thenReturn(new ApproverAnnouncementReviewRow(4, 1, 9));
        when(approverReviewDao.selectVerificationReview()).thenReturn(new ApproverVerificationReviewRow(5, 2, 8, 1));
        when(approverReviewDao.selectMatchingReview()).thenReturn(new ApproverMatchingReviewRow(3, 1, 7));
        when(approverReviewDao.selectProgressReview()).thenReturn(new ApproverProgressReviewRow(6, 4, 2, 1));

        ApproverReviewSummaryResponse response = approverReviewService.selectSummary();

        assertThat(response.announcementReview().requestedCount()).isEqualTo(4);
        assertThat(response.verificationReview().reviewingCount()).isEqualTo(2);
        assertThat(response.matchingReview().reviewRequiredCount()).isEqualTo(3);
        assertThat(response.progressReview().waitingResultCount()).isEqualTo(6);
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectSummaryReturnsZeroCountsWhenDaoRowsAreNull() {
        ApproverReviewSummaryResponse response = approverReviewService.selectSummary();

        assertThat(response.announcementReview().requestedCount()).isZero();
        assertThat(response.verificationReview().submittedCount()).isZero();
        assertThat(response.matchingReview().reviewRequiredCount()).isZero();
        assertThat(response.progressReview().waitingResultCount()).isZero();
    }
}

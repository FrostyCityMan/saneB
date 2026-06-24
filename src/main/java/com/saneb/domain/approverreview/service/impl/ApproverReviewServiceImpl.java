/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApproverReviewServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.approverreview.service.impl;

import com.saneb.domain.approverreview.dao.ApproverReviewDao;
import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse;
import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse.AnnouncementReviewResponse;
import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse.MatchingReviewResponse;
import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse.ProgressReviewResponse;
import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse.VerificationReviewResponse;
import com.saneb.domain.approverreview.service.ApproverReviewService;
import com.saneb.domain.approverreview.vo.ApproverAnnouncementReviewRow;
import com.saneb.domain.approverreview.vo.ApproverMatchingReviewRow;
import com.saneb.domain.approverreview.vo.ApproverProgressReviewRow;
import com.saneb.domain.approverreview.vo.ApproverVerificationReviewRow;
import org.springframework.stereotype.Service;

@Service
public class ApproverReviewServiceImpl implements ApproverReviewService {

    private final ApproverReviewDao approverReviewDao;

    /**
     * 객체를 생성합니다.
     *
     * @param approverReviewDao 입력 값
     */
    public ApproverReviewServiceImpl(ApproverReviewDao approverReviewDao) {
        this.approverReviewDao = approverReviewDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    @Override
    public ApproverReviewSummaryResponse selectSummary() {
        ApproverAnnouncementReviewRow announcementReview = nullToEmpty(approverReviewDao.selectAnnouncementReview());
        ApproverVerificationReviewRow verificationReview = nullToEmpty(approverReviewDao.selectVerificationReview());
        ApproverMatchingReviewRow matchingReview = nullToEmpty(approverReviewDao.selectMatchingReview());
        ApproverProgressReviewRow progressReview = nullToEmpty(approverReviewDao.selectProgressReview());

        return new ApproverReviewSummaryResponse(
                new AnnouncementReviewResponse(
                        announcementReview.requestedCount(),
                        announcementReview.rejectedCount(),
                        announcementReview.approvedCount()
                ),
                new VerificationReviewResponse(
                        verificationReview.submittedCount(),
                        verificationReview.reviewingCount(),
                        verificationReview.verifiedCount(),
                        verificationReview.rejectedCount()
                ),
                new MatchingReviewResponse(
                        matchingReview.reviewRequiredCount(),
                        matchingReview.blockedCount(),
                        matchingReview.progressedCount()
                ),
                new ProgressReviewResponse(
                        progressReview.waitingResultCount(),
                        progressReview.approvedCount(),
                        progressReview.supplementRequestedCount(),
                        progressReview.stoppedCount()
                )
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApproverAnnouncementReviewRow nullToEmpty(ApproverAnnouncementReviewRow row) {
        return row == null ? new ApproverAnnouncementReviewRow(0, 0, 0) : row;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApproverVerificationReviewRow nullToEmpty(ApproverVerificationReviewRow row) {
        return row == null ? new ApproverVerificationReviewRow(0, 0, 0, 0) : row;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApproverMatchingReviewRow nullToEmpty(ApproverMatchingReviewRow row) {
        return row == null ? new ApproverMatchingReviewRow(0, 0, 0) : row;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private ApproverProgressReviewRow nullToEmpty(ApproverProgressReviewRow row) {
        return row == null ? new ApproverProgressReviewRow(0, 0, 0, 0) : row;
    }
}

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

    public ApproverReviewServiceImpl(ApproverReviewDao approverReviewDao) {
        this.approverReviewDao = approverReviewDao;
    }

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

    private ApproverAnnouncementReviewRow nullToEmpty(ApproverAnnouncementReviewRow row) {
        return row == null ? new ApproverAnnouncementReviewRow(0, 0, 0) : row;
    }

    private ApproverVerificationReviewRow nullToEmpty(ApproverVerificationReviewRow row) {
        return row == null ? new ApproverVerificationReviewRow(0, 0, 0, 0) : row;
    }

    private ApproverMatchingReviewRow nullToEmpty(ApproverMatchingReviewRow row) {
        return row == null ? new ApproverMatchingReviewRow(0, 0, 0) : row;
    }

    private ApproverProgressReviewRow nullToEmpty(ApproverProgressReviewRow row) {
        return row == null ? new ApproverProgressReviewRow(0, 0, 0, 0) : row;
    }
}

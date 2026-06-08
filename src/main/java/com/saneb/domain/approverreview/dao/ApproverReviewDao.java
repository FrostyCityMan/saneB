package com.saneb.domain.approverreview.dao;

import com.saneb.domain.approverreview.vo.ApproverAnnouncementReviewRow;
import com.saneb.domain.approverreview.vo.ApproverMatchingReviewRow;
import com.saneb.domain.approverreview.vo.ApproverProgressReviewRow;
import com.saneb.domain.approverreview.vo.ApproverVerificationReviewRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApproverReviewDao {

    ApproverAnnouncementReviewRow selectAnnouncementReview();

    ApproverVerificationReviewRow selectVerificationReview();

    ApproverMatchingReviewRow selectMatchingReview();

    ApproverProgressReviewRow selectProgressReview();
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApproverReviewDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.approverreview.dao;

import com.saneb.domain.approverreview.vo.ApproverAnnouncementReviewRow;
import com.saneb.domain.approverreview.vo.ApproverMatchingReviewRow;
import com.saneb.domain.approverreview.vo.ApproverProgressReviewRow;
import com.saneb.domain.approverreview.vo.ApproverVerificationReviewRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApproverReviewDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    ApproverAnnouncementReviewRow selectAnnouncementReview();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    ApproverVerificationReviewRow selectVerificationReview();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    ApproverMatchingReviewRow selectMatchingReview();

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    ApproverProgressReviewRow selectProgressReview();
}

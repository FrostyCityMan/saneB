/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApproverReviewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.approverreview.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse;
import com.saneb.domain.approverreview.service.ApproverReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approver/reviews")
public class ApproverReviewController {

    private final ApproverReviewService approverReviewService;

    public ApproverReviewController(ApproverReviewService approverReviewService) {
        this.approverReviewService = approverReviewService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApiResponse<ApproverReviewSummaryResponse> selectSummary() {
        return ApiResponse.success(approverReviewService.selectSummary());
    }
}

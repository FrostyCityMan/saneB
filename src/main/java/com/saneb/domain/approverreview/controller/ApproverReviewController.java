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

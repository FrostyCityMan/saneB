/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApproverReviewViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.approverreview.controller;

import com.saneb.domain.approverreview.dto.ApproverReviewSummaryResponse;
import com.saneb.domain.approverreview.service.ApproverReviewService;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApproverReviewViewController {

    private final AuthService authService;
    private final ApproverReviewService approverReviewService;

    public ApproverReviewViewController(
            AuthService authService,
            ApproverReviewService approverReviewService
    ) {
        this.authService = authService;
        this.approverReviewService = approverReviewService;
    }

    @GetMapping("/app/approver/reviews")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public String selectApproverReviewPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        ApproverReviewSummaryResponse summary = approverReviewService.selectSummary();
        model.addAttribute("page", ApproverReviewPageModel.from(authMe, summary));
        return "app/approver-reviews";
    }

    public record ApproverReviewPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            ApproverReviewSummaryResponse summary,
            int totalReviewCount
    ) {

        private static ApproverReviewPageModel from(
                AuthMeResponse auth,
                ApproverReviewSummaryResponse summary
        ) {
            int totalReviewCount = summary.announcementReview().requestedCount()
                    + summary.verificationReview().submittedCount()
                    + summary.verificationReview().reviewingCount()
                    + summary.matchingReview().reviewRequiredCount()
                    + summary.progressReview().waitingResultCount();
            return new ApproverReviewPageModel(
                    auth,
                    ApproverReviewViewController.roleLabel(auth.primaryRole()),
                    "APPROVER_REVIEWS",
                    summary,
                    totalReviewCount
            );
        }
    }

    private static String roleLabel(String code) {
        return switch (code) {
            case "ADMIN" -> "관리자";
            case "APPROVER" -> "승인자";
            case "OPERATOR" -> "운영자";
            case "PARTNER" -> "파트너";
            default -> "일반 사용자";
        };
    }
}

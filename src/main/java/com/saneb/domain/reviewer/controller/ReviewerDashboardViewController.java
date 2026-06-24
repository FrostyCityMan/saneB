/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ReviewerDashboardViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.reviewer.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReviewerDashboardViewController {

    private final AuthService authService;

    public ReviewerDashboardViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/app/reviewer/dashboard")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public String selectReviewerDashboardPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", ReviewerDashboardPageModel.from(authMe));
        return "app/reviewer-dashboard";
    }

    public record ReviewerDashboardPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            List<ReviewerLinkModel> links
    ) {

        private static ReviewerDashboardPageModel from(AuthMeResponse auth) {
            return new ReviewerDashboardPageModel(
                    auth,
                    selectRoleLabel(auth.primaryRole()),
                    "REVIEWER_DASHBOARD",
                    List.of(
                            new ReviewerLinkModel("검증 목록", "/app/partner/verifications", "검증 상태와 서류 확인 흐름을 조회합니다."),
                            new ReviewerLinkModel("매칭 관리", "/app/matching/cases", "공고와 회원의 매칭 생성 결과를 조회합니다."),
                            new ReviewerLinkModel("진행 현황", "/app/application-progresses", "공고 신청 단계와 처리 결과를 조회합니다.")
                    )
            );
        }
    }

    public record ReviewerLinkModel(
            String label,
            String href,
            String description
    ) {
    }

    private static String selectRoleLabel(String code) {
        return switch (code) {
            case "ADMIN" -> "관리자";
            case "REVIEWER" -> "검수자";
            default -> "검수자";
        };
    }
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: OperatorDashboardViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operatordashboard.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse;
import com.saneb.domain.operatordashboard.service.OperatorDashboardService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OperatorDashboardViewController {

    private static final NumberFormat KOREAN_NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final AuthService authService;
    private final OperatorDashboardService operatorDashboardService;

    public OperatorDashboardViewController(
            AuthService authService,
            OperatorDashboardService operatorDashboardService
    ) {
        this.authService = authService;
        this.operatorDashboardService = operatorDashboardService;
    }

    @GetMapping("/app/operator/dashboard")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public String selectOperatorDashboardPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        OperatorDashboardSummaryResponse summary = operatorDashboardService.selectSummary();
        model.addAttribute("page", OperatorDashboardPageModel.from(authMe, summary));
        return "app/operator-dashboard";
    }

    public record OperatorDashboardPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            OperatorDashboardSummaryResponse summary,
            String totalReceivedAmountText
    ) {

        private static OperatorDashboardPageModel from(
                AuthMeResponse auth,
                OperatorDashboardSummaryResponse summary
        ) {
            return new OperatorDashboardPageModel(
                    auth,
                    OperatorDashboardViewController.roleLabel(auth.primaryRole()),
                    "DASHBOARD",
                    summary,
                    wonText(summary.applicationProgressWork().totalReceivedAmount())
            );
        }
    }

    private static String wonText(BigDecimal amount) {
        if (amount == null) {
            return "0원";
        }
        return KOREAN_NUMBER_FORMAT.format(amount) + "원";
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

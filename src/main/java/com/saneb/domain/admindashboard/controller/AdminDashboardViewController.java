/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminDashboardViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.admindashboard.controller;

import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.StatusCountResponse;
import com.saneb.domain.admindashboard.service.AdminDashboardService;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceAutomationStatusResponse;
import com.saneb.domain.announcementsource.localgov.dto.LocalGovernmentNoticeCollectionSummaryResponse;
import com.saneb.domain.announcementsource.localgov.service.LocalGovernmentNoticeService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceAutomationStatusService;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardViewController {

    private static final NumberFormat KOREAN_NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final AuthService authService;
    private final AdminDashboardService adminDashboardService;
    private final LocalGovernmentNoticeService localGovernmentNoticeService;
    private final AnnouncementSourceAutomationStatusService automationStatusService;

    public AdminDashboardViewController(
            AuthService authService,
            AdminDashboardService adminDashboardService,
            LocalGovernmentNoticeService localGovernmentNoticeService,
            AnnouncementSourceAutomationStatusService automationStatusService
    ) {
        this.authService = authService;
        this.adminDashboardService = adminDashboardService;
        this.localGovernmentNoticeService = localGovernmentNoticeService;
        this.automationStatusService = automationStatusService;
    }

    @GetMapping("/app/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String selectAdminDashboardPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        AdminDashboardSummaryResponse summary = adminDashboardService.selectSummary();
        LocalGovernmentNoticeCollectionSummaryResponse collectionSummary =
                localGovernmentNoticeService.selectCollectionSummary();
        AnnouncementSourceAutomationStatusResponse automationStatus = automationStatusService.selectStatus();
        model.addAttribute("page", AdminDashboardPageModel.from(
                authMe,
                summary,
                collectionSummary,
                automationStatus
        ));
        return "app/admin-dashboard";
    }

    public record AdminDashboardPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            AdminDashboardSummaryResponse summary,
            LocalGovernmentNoticeCollectionSummaryResponse collectionSummary,
            AnnouncementSourceAutomationStatusResponse automationStatus,
            String collectionTrafficLabel,
            String collectionTrafficClass,
            String totalReceivedAmountText,
            List<StatusMetricModel> verificationStatusMetrics,
            List<StatusMetricModel> matchingStatusMetrics,
            List<StatusMetricModel> progressStatusMetrics
    ) {

        private static AdminDashboardPageModel from(
                AuthMeResponse auth,
                AdminDashboardSummaryResponse summary,
                LocalGovernmentNoticeCollectionSummaryResponse collectionSummary,
                AnnouncementSourceAutomationStatusResponse automationStatus
        ) {
            return new AdminDashboardPageModel(
                    auth,
                    AdminDashboardViewController.roleLabel(auth.primaryRole()),
                    "DASHBOARD",
                    summary,
                    collectionSummary,
                    automationStatus,
                    collectionTrafficLabel(collectionSummary.trafficLightCode()),
                    collectionTrafficClass(collectionSummary.trafficLightCode()),
                    wonText(summary.applicationProgressSummary().totalReceivedAmount()),
                    summary.verificationSummary().statusCounts().stream()
                            .map(statusCount -> StatusMetricModel.from(
                                    partnerVerificationStatusLabel(statusCount.statusCode()),
                                    statusCount
                            ))
                            .toList(),
                    summary.matchingSummary().statusCounts().stream()
                            .map(statusCount -> StatusMetricModel.from(
                                    matchingStatusLabel(statusCount.statusCode()),
                                    statusCount
                            ))
                            .toList(),
                    summary.applicationProgressSummary().statusCounts().stream()
                            .map(statusCount -> StatusMetricModel.from(
                                    applicationProgressStatusLabel(statusCount.statusCode()),
                                    statusCount
                            ))
                            .toList()
            );
        }

        private static String collectionTrafficLabel(String code) {
            return switch (code) {
                case "RED" -> "수집 오류 확인 필요";
                case "YELLOW" -> "신규·미처리 항목 확인 필요";
                default -> "지자체 공고 수집 정상";
            };
        }

        private static String collectionTrafficClass(String code) {
            return switch (code) {
                case "RED" -> "signal-red";
                case "GREEN" -> "signal-green";
                default -> "signal-yellow";
            };
        }
    }

    public record StatusMetricModel(
            String label,
            String statusCode,
            int count
    ) {

        private static StatusMetricModel from(String label, StatusCountResponse statusCount) {
            return new StatusMetricModel(label, statusCount.statusCode(), statusCount.count());
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

    private static String partnerVerificationStatusLabel(String code) {
        return switch (code) {
            case "SUBMITTED" -> "제출 완료";
            case "REVIEWING" -> "검토 중";
            case "VERIFIED" -> "검증 완료";
            case "REJECTED" -> "반려";
            case "EXPIRED" -> "만료";
            default -> "작성 중";
        };
    }

    private static String matchingStatusLabel(String code) {
        return switch (code) {
            case "NOT_MATCHED" -> "미매칭";
            case "REVIEW_REQUIRED" -> "검토 필요";
            case "BLOCKED" -> "차단";
            case "PROGRESSED" -> "진행 전환";
            default -> "매칭";
        };
    }

    private static String applicationProgressStatusLabel(String code) {
        return switch (code) {
            case "IN_PROGRESS" -> "진행 중";
            case "WAITING_RESULT" -> "결과 대기";
            case "APPROVED" -> "승인";
            case "REJECTED" -> "반려";
            case "SUPPLEMENT_REQUESTED" -> "보완 요청";
            case "STOPPED" -> "중단";
            case "COMPLETED" -> "완료";
            default -> "준비";
        };
    }
}

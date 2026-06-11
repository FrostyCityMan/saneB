package com.saneb.domain.dashboard.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    private static final NumberFormat KOREAN_NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final AuthService authService;
    private final DashboardService dashboardService;

    public DashboardViewController(AuthService authService, DashboardService dashboardService) {
        this.authService = authService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/app")
    public String redirectDefaultRoute(Authentication authentication) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        return "redirect:" + authMe.defaultRoute();
    }

    @GetMapping("/app/dashboard")
    public String selectDashboardPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        if (!isUserDashboard(authMe)) {
            model.addAttribute("page", DashboardPageModel.operating(authMe));
            return "app/dashboard";
        }

        DashboardSummaryResponse summary = dashboardService.selectMySummary(authentication);
        DashboardCurrentActionResponse currentAction = dashboardService.selectMyCurrentAction(authentication);
        DashboardProgressSummaryResponse progressSummary = dashboardService.selectMyProgressSummary(authentication);

        model.addAttribute("page", DashboardPageModel.from(
                authMe,
                summary,
                currentAction,
                progressSummary
        ));
        return "app/dashboard";
    }

    public record DashboardPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            boolean userDashboard,
            String serviceStatusLabel,
            String verificationStatusLabel,
            String noticeMessage,
            List<MetricModel> candidateMetrics,
            String supportAmountRangeText,
            int finalMatchedCount,
            ActionModel currentAction,
            List<MetricModel> progressMetrics,
            String totalReceivedAmountText
    ) {

        private static DashboardPageModel from(
                AuthMeResponse auth,
                DashboardSummaryResponse summary,
                DashboardCurrentActionResponse currentAction,
                DashboardProgressSummaryResponse progressSummary
        ) {
            return new DashboardPageModel(
                    auth,
                    DashboardViewController.roleLabel(auth.primaryRole()),
                    "DASHBOARD",
                    true,
                    DashboardViewController.serviceStatusLabel(summary.serviceStatusCode()),
                    DashboardViewController.verificationStatusLabel(summary.verificationStatusCode()),
                    summary.noticeMessage(),
                    List.of(
                            new MetricModel("사업자 기준", summary.targetCandidateCounts().business(), "building"),
                            new MetricModel("개인 기준", summary.targetCandidateCounts().personal(), "user"),
                            new MetricModel("가족 기준", summary.targetCandidateCounts().family(), "family")
                    ),
                    amountRangeText(summary.supportAmountRange()),
                    summary.finalMatchedCount(),
                    ActionModel.from(currentAction),
                    List.of(
                            new MetricModel("진행 중", progressSummary.inProgressCount(), "progress"),
                            new MetricModel("결과 대기", progressSummary.waitingResultCount(), "clock"),
                            new MetricModel("승인", progressSummary.approvedCount(), "check"),
                            new MetricModel("보완 요청", progressSummary.supplementRequestedCount(), "alert"),
                            new MetricModel("중단", progressSummary.stoppedCount(), "stop")
                    ),
                    wonText(progressSummary.totalReceivedAmount())
            );
        }

        private static DashboardPageModel operating(AuthMeResponse auth) {
            return new DashboardPageModel(
                    auth,
                    DashboardViewController.roleLabel(auth.primaryRole()),
                    "DASHBOARD",
                    false,
                    "운영 계정",
                    "해당 없음",
                    "운영 계정은 사용자 검증 흐름과 분리되어 있습니다.",
                    List.of(
                            new MetricModel("사업자 기준", 0, "building"),
                            new MetricModel("개인 기준", 0, "user"),
                            new MetricModel("가족 기준", 0, "family")
                    ),
                    "확인 대기",
                    0,
                    ActionModel.none(),
                    List.of(
                            new MetricModel("진행 중", 0, "progress"),
                            new MetricModel("결과 대기", 0, "clock"),
                            new MetricModel("승인", 0, "check"),
                            new MetricModel("보완 요청", 0, "alert"),
                            new MetricModel("중단", 0, "stop")
                    ),
                    "0원"
            );
        }
    }

    public record MetricModel(String label, int value, String icon) {
    }

    public record ActionModel(
            String actionCode,
            String title,
            String description,
            String primaryButtonLabel,
            String route,
            String dueDateText
    ) {

        private static ActionModel from(DashboardCurrentActionResponse action) {
            return new ActionModel(
                    action.actionCode(),
                    action.title(),
                    action.description(),
                    action.primaryButtonLabel(),
                    action.route(),
                    action.dueDate() == null ? "권장 완료일 미지정" : action.dueDate().toString()
            );
        }

        private static ActionModel none() {
            return new ActionModel(
                    "NONE",
                    "처리할 사용자 행동 없음",
                    "운영 계정에는 사용자 행동 카드가 표시되지 않습니다.",
                    null,
                    null,
                    "권장 완료일 미지정"
            );
        }
    }

    private static boolean isUserDashboard(AuthMeResponse authMe) {
        return "USER".equals(authMe.primaryRole());
    }

    private static String amountRangeText(DashboardSummaryResponse.SupportAmountRangeResponse range) {
        if (range == null || range.minAmount() == null || range.maxAmount() == null) {
            return "확인 대기";
        }
        return wonText(range.minAmount()) + " ~ " + wonText(range.maxAmount());
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

    private static String serviceStatusLabel(String code) {
        return switch (code) {
            case "MATCHING_READY" -> "매칭 준비";
            case "IN_PROGRESS" -> "진행 중";
            case "WAITING_RESULT" -> "결과 대기";
            case "COMPLETED" -> "완료";
            case "BASIC_INFO_REQUIRED" -> "기본 정보 필요";
            default -> "진행 준비";
        };
    }

    private static String verificationStatusLabel(String code) {
        return switch (code) {
            case "SUBMITTED" -> "제출 완료";
            case "REVIEWING" -> "검토 중";
            case "VERIFIED" -> "검증 완료";
            case "REJECTED" -> "반려";
            case "EXPIRED" -> "만료";
            default -> "검증 전";
        };
    }

}

package com.saneb.domain.dashboard.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    private static final NumberFormat KOREAN_NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
        DashboardSummaryResponse summary = dashboardService.selectMySummary(authentication);
        DashboardCurrentActionResponse currentAction = dashboardService.selectMyCurrentAction(authentication);
        DashboardProgressSummaryResponse progressSummary = dashboardService.selectMyProgressSummary(authentication);
        DashboardReverificationStatusResponse reverificationStatus = dashboardService.selectMyReverificationStatus(authentication);

        model.addAttribute("page", DashboardPageModel.from(
                authMe,
                summary,
                currentAction,
                progressSummary,
                reverificationStatus
        ));
        return "app/dashboard";
    }

    public record DashboardPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            String serviceStatusLabel,
            String verificationStatusLabel,
            String noticeMessage,
            List<MetricModel> candidateMetrics,
            String supportAmountRangeText,
            int finalMatchedCount,
            ActionModel currentAction,
            List<MetricModel> progressMetrics,
            String totalReceivedAmountText,
            ReverificationModel reverification
    ) {

        private static DashboardPageModel from(
                AuthMeResponse auth,
                DashboardSummaryResponse summary,
                DashboardCurrentActionResponse currentAction,
                DashboardProgressSummaryResponse progressSummary,
                DashboardReverificationStatusResponse reverificationStatus
        ) {
            return new DashboardPageModel(
                    auth,
                    DashboardViewController.roleLabel(auth.primaryRole()),
                    "DASHBOARD",
                    DashboardViewController.serviceStatusLabel(summary.serviceStatusCode()),
                    DashboardViewController.verificationStatusLabel(summary.verificationStatusCode()),
                    summary.noticeMessage(),
                    List.of(
                            new MetricModel("정책자금 후보", summary.candidateCounts().policyFund(), "building"),
                            new MetricModel("지원금 후보", summary.candidateCounts().supportFund(), "gift"),
                            new MetricModel("보조금 후보", summary.candidateCounts().subsidy(), "hand")
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
                    wonText(progressSummary.totalReceivedAmount()),
                    ReverificationModel.from(reverificationStatus)
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
    }

    public record ReverificationModel(
            boolean required,
            String title,
            String description,
            String lastVerifiedAtText,
            List<String> requiredItems
    ) {

        private static ReverificationModel from(DashboardReverificationStatusResponse status) {
            if (!status.required()) {
                return new ReverificationModel(
                        false,
                        "재검증 필요 상태가 없습니다.",
                        "최근 검증값 기준으로 추가 확인 요청이 없습니다.",
                        status.lastVerifiedAt() == null ? "검증 이력 없음" : status.lastVerifiedAt().format(DATE_TIME_FORMAT),
                        List.of()
                );
            }

            return new ReverificationModel(
                    true,
                    "재검증이 필요합니다.",
                    reasonLabel(status.reasonCode()),
                    status.lastVerifiedAt() == null ? "검증 이력 없음" : status.lastVerifiedAt().format(DATE_TIME_FORMAT),
                    status.requiredItems().stream()
                            .map(DashboardViewController::requiredItemLabel)
                            .toList()
            );
        }
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
            default -> "검증 필요";
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

    private static String reasonLabel(String code) {
        return switch (code) {
            case "BUSINESS_STATUS_CHANGED" -> "사업 상태 변경 가능성이 있어 최신 상태 확인이 필요합니다.";
            case "TAX_STATUS_REQUIRED" -> "세금 상태 확인이 필요합니다.";
            case "FINANCIAL_STATUS_REQUIRED" -> "금융 상태 확인이 필요합니다.";
            default -> "검증 기준일이 경과하여 최신 정보 확인이 필요합니다.";
        };
    }

    private static String requiredItemLabel(String code) {
        return switch (code) {
            case "BUSINESS_STATUS" -> "사업 상태";
            case "TAX_STATUS" -> "세금 상태";
            case "FINANCIAL_STATUS" -> "금융 상태";
            case "FAMILY_STATUS" -> "가족 정보";
            default -> code;
        };
    }
}

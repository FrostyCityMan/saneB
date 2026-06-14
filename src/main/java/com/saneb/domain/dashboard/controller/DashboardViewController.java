package com.saneb.domain.dashboard.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.service.MatchingService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
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
    private final MatchingService matchingService;

    public DashboardViewController(
            AuthService authService,
            DashboardService dashboardService,
            MatchingService matchingService
    ) {
        this.authService = authService;
        this.dashboardService = dashboardService;
        this.matchingService = matchingService;
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
        List<MatchingCaseSummaryResponse> basicCandidates =
                matchingService.selectMyBasicMatchingCaseList(authentication, 1, 5).items();

        model.addAttribute("page", DashboardPageModel.from(
                authMe,
                summary,
                currentAction,
                progressSummary,
                basicCandidates
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
            List<CandidateItemModel> candidateItems,
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
                DashboardProgressSummaryResponse progressSummary,
                List<MatchingCaseSummaryResponse> basicCandidates
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
                    basicCandidates.stream().map(CandidateItemModel::from).toList(),
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
                    List.of(),
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

    public record CandidateItemModel(
            String announcementCode,
            String title,
            String agencyName,
            String targetLabel,
            String amountRangeText,
            String periodText,
            String statusLabel
    ) {

        private static CandidateItemModel from(MatchingCaseSummaryResponse response) {
            return new CandidateItemModel(
                    response.announcementCode() == null ? "공고 코드 없음" : response.announcementCode(),
                    response.announcementTitle() == null ? "공고명 미입력" : response.announcementTitle(),
                    response.agencyName() == null ? "기관 미입력" : response.agencyName(),
                    DashboardViewController.targetLabel(response.targetTypeCode()),
                    DashboardViewController.amountRangeText(response.minAmount(), response.maxAmount()),
                    DashboardViewController.periodText(response.applicationStartDate(), response.applicationEndDate()),
                    DashboardViewController.matchingStatusLabel(response.statusCode())
            );
        }
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

    private static String amountRangeText(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount == null && maxAmount == null) {
            return "금액 미입력";
        }
        String minText = minAmount == null ? "하한 없음" : wonText(minAmount);
        String maxText = maxAmount == null ? "상한 없음" : wonText(maxAmount);
        return minText + " ~ " + maxText;
    }

    private static String periodText(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "기간 미입력";
        }
        return (startDate == null ? "시작일 미입력" : startDate.toString())
                + " ~ "
                + (endDate == null ? "마감일 미입력" : endDate.toString());
    }

    private static String targetLabel(String code) {
        return switch (code == null ? "" : code) {
            case "BUSINESS" -> "사업자";
            case "PERSONAL" -> "개인";
            case "SPOUSE", "CHILD", "PARENT", "FAMILY" -> "가족";
            default -> "구분 미입력";
        };
    }

    private static String matchingStatusLabel(String code) {
        return switch (code == null ? "" : code) {
            case "MATCHED" -> "기본정보 일치";
            case "REVIEW_REQUIRED" -> "확인 필요";
            case "PROGRESSED" -> "진행 전환";
            case "NOT_MATCHED" -> "미매칭";
            case "BLOCKED" -> "제외";
            default -> "상태 미입력";
        };
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
            case "SUBSCRIPTION_REQUIRED" -> "구독 필요";
            case "CONSULTATION_REQUEST_REQUIRED" -> "상담 요청 필요";
            case "FINAL_MATCHING_WAITING" -> "최종 확인 중";
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

package com.saneb.domain.matching.controller;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.service.MatchingService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MatchingViewController {

    private static final NumberFormat KOREAN_NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    private final AuthService authService;
    private final MatchingService matchingService;

    public MatchingViewController(AuthService authService, MatchingService matchingService) {
        this.authService = authService;
        this.matchingService = matchingService;
    }

    @GetMapping("/app/matching/cases")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public String selectMatchingCasePage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", MatchingCasePageModel.from(authMe));
        return "app/matching-cases";
    }

    @GetMapping("/app/matching/basic-candidates")
    @PreAuthorize("hasRole('USER')")
    public String selectBasicMatchingCandidatePage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PageResponse<MatchingCaseSummaryResponse> candidates =
                matchingService.selectMyBasicMatchingCaseList(authentication, 1, 50);
        model.addAttribute("page", BasicMatchingCandidatePageModel.from(authMe, candidates));
        return "app/basic-matching-candidates";
    }

    public record MatchingCasePageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            boolean canOperate
    ) {

        private static MatchingCasePageModel from(AuthMeResponse auth) {
            return new MatchingCasePageModel(
                    auth,
                    MatchingViewController.roleLabel(auth.primaryRole()),
                    "MATCHING_CASES",
                    auth.roles().stream().anyMatch(role -> java.util.Set.of("OPERATOR", "ADMIN").contains(role))
            );
        }
    }

    public record BasicMatchingCandidatePageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            List<BasicCandidateItemModel> candidates,
            long totalCount,
            boolean hasCandidates,
            String amountRangeText
    ) {

        private static BasicMatchingCandidatePageModel from(
                AuthMeResponse auth,
                PageResponse<MatchingCaseSummaryResponse> candidates
        ) {
            List<BasicCandidateItemModel> items = candidates.items().stream()
                    .map(BasicCandidateItemModel::from)
                    .toList();
            return new BasicMatchingCandidatePageModel(
                    auth,
                    MatchingViewController.roleLabel(auth.primaryRole()),
                    "BASIC_MATCHING_CANDIDATES",
                    items,
                    candidates.totalCount(),
                    !items.isEmpty(),
                    aggregateAmountRangeText(candidates.items())
            );
        }
    }

    public record BasicCandidateItemModel(
            String matchingCaseCode,
            String announcementCode,
            String title,
            String agencyName,
            String targetLabel,
            String amountRangeText,
            String periodText,
            String statusLabel
    ) {

        private static BasicCandidateItemModel from(MatchingCaseSummaryResponse response) {
            return new BasicCandidateItemModel(
                    response.matchingCaseCode() == null ? "매칭 코드 없음" : response.matchingCaseCode(),
                    response.announcementCode() == null ? "공고 코드 없음" : response.announcementCode(),
                    response.announcementTitle() == null ? "공고명 미입력" : response.announcementTitle(),
                    response.agencyName() == null ? "기관 미입력" : response.agencyName(),
                    MatchingViewController.targetLabel(response.targetTypeCode()),
                    MatchingViewController.amountRangeText(response.minAmount(), response.maxAmount()),
                    MatchingViewController.periodText(response.applicationStartDate(), response.applicationEndDate()),
                    MatchingViewController.matchingStatusLabel(response.statusCode())
            );
        }
    }

    private static String aggregateAmountRangeText(List<MatchingCaseSummaryResponse> items) {
        BigDecimal minAmount = items.stream()
                .map(MatchingCaseSummaryResponse::minAmount)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        BigDecimal maxAmount = items.stream()
                .map(MatchingCaseSummaryResponse::maxAmount)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
        if (minAmount == null && maxAmount == null) {
            return "확인 대기";
        }
        return amountRangeText(minAmount, maxAmount);
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
            case "REVIEWER" -> "검수자";
            case "PARTNER" -> "파트너";
            default -> "일반 사용자";
        };
    }
}

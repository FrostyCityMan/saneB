package com.saneb.domain.dashboard.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dashboard.dao.DashboardDao;
import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
import com.saneb.domain.dashboard.vo.DashboardCandidateSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardCurrentStepRow;
import com.saneb.domain.dashboard.vo.DashboardProgressSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardVerificationStatusRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String BASIS_ANNOUNCEMENT_AMOUNT_RANGE = "ANNOUNCEMENT_AMOUNT_RANGE";
    private static final String VERIFICATION_REQUIRED = "VERIFICATION_REQUIRED";
    private static final String VERIFICATION_DOCUMENT_REQUIRED = "VERIFICATION_DOCUMENT_REQUIRED";
    private static final String PROGRESS_ACTION_REQUIRED = "PROGRESS_ACTION_REQUIRED";
    private static final String NO_ACTION = "NONE";

    private final DashboardDao dashboardDao;

    public DashboardServiceImpl(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    @Override
    public DashboardSummaryResponse selectMySummary(Authentication authentication) {
        UUID userId = selectCurrentUserId(authentication);
        DashboardVerificationStatusRow verification = selectVerificationStatus(userId);
        DashboardCandidateSummaryRow candidateSummary = selectCandidateSummary(userId);
        DashboardProgressSummaryRow progressSummary = selectProgressSummary(userId);

        return new DashboardSummaryResponse(
                selectServiceStatusCode(verification, progressSummary, candidateSummary),
                new DashboardSummaryResponse.CandidateCountsResponse(
                        candidateSummary.policyFundCount(),
                        candidateSummary.supportFundCount(),
                        candidateSummary.subsidyCount()
                ),
                candidateSummary.finalMatchedCount(),
                new DashboardSummaryResponse.SupportAmountRangeResponse(
                        candidateSummary.minAmount(),
                        candidateSummary.maxAmount(),
                        BASIS_ANNOUNCEMENT_AMOUNT_RANGE
                ),
                selectVerificationStatusCode(verification),
                selectNoticeMessage(verification, candidateSummary)
        );
    }

    @Override
    public DashboardCurrentActionResponse selectMyCurrentAction(Authentication authentication) {
        UUID userId = selectCurrentUserId(authentication);
        DashboardCurrentStepRow currentStep = dashboardDao.selectCurrentStepDetails(userId);
        if (currentStep != null) {
            return new DashboardCurrentActionResponse(
                    PROGRESS_ACTION_REQUIRED,
                    currentStep.stepName(),
                    selectCurrentStepDescription(currentStep),
                    currentStep.buttonLabel() == null || currentStep.buttonLabel().isBlank()
                            ? "진행 확인하기"
                            : currentStep.buttonLabel(),
                    "/app/application-progresses/" + currentStep.progressId(),
                    currentStep.dueDate(),
                    selectStepDisplayOrder(currentStep.stepStatusCode())
            );
        }

        DashboardVerificationStatusRow verification = selectVerificationStatus(userId);
        DashboardCandidateSummaryRow candidateSummary = selectCandidateSummary(userId);
        if (hasStartableMatching(candidateSummary)) {
            return new DashboardCurrentActionResponse(
                    "APPLICATION_START_AVAILABLE",
                    "신청 가능한 공고가 있습니다.",
                    "검증이 완료되지 않아도 신청 가능한 공고는 바로 진행을 시작할 수 있습니다.",
                    "신청 진행하기",
                    "/app/application-progresses",
                    null,
                    15
            );
        }
        if (!"VERIFIED".equals(selectVerificationStatusCode(verification))) {
            return new DashboardCurrentActionResponse(
                    VERIFICATION_DOCUMENT_REQUIRED,
                    "전자증명 검증이 필요합니다.",
                    "최종 매칭 전 파트너 검증과 필수 서류 확인이 필요합니다.",
                    "검증 진행하기",
                    "/app/member/verifications/current",
                    null,
                    5
            );
        }

        return new DashboardCurrentActionResponse(
                NO_ACTION,
                "현재 처리할 진행 행동이 없습니다.",
                "READY 또는 IN_PROGRESS 상태의 진행 단계가 없습니다.",
                null,
                null,
                null,
                99
        );
    }

    @Override
    public DashboardProgressSummaryResponse selectMyProgressSummary(Authentication authentication) {
        DashboardProgressSummaryRow summary = selectProgressSummary(selectCurrentUserId(authentication));
        return new DashboardProgressSummaryResponse(
                summary.inProgressCount(),
                summary.waitingResultCount(),
                summary.approvedCount(),
                summary.supplementRequestedCount(),
                summary.stoppedCount(),
                summary.totalReceivedAmount() == null ? BigDecimal.ZERO : summary.totalReceivedAmount()
        );
    }

    @Override
    public DashboardReverificationStatusResponse selectMyReverificationStatus(Authentication authentication) {
        DashboardVerificationStatusRow verification = selectVerificationStatus(selectCurrentUserId(authentication));
        if (verification == null) {
            return new DashboardReverificationStatusResponse(false, null, null, List.of());
        }
        if ("EXPIRED".equals(verification.statusCode())) {
            return new DashboardReverificationStatusResponse(
                    true,
                    verification.verifiedAt(),
                    "VERIFICATION_EXPIRED",
                    List.of("BUSINESS_STATUS", "TAX_STATUS", "FINANCIAL_STATUS")
            );
        }
        return new DashboardReverificationStatusResponse(false, verification.verifiedAt(), null, List.of());
    }

    private UUID selectCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal.userId();
        }
        return dashboardDao.selectUserIdByLoginId(authentication.getName());
    }

    private DashboardVerificationStatusRow selectVerificationStatus(UUID userId) {
        if (userId == null) {
            return null;
        }
        return dashboardDao.selectCurrentVerificationStatus(userId);
    }

    private DashboardCandidateSummaryRow selectCandidateSummary(UUID userId) {
        if (userId == null) {
            return emptyCandidateSummary();
        }
        DashboardCandidateSummaryRow row = dashboardDao.selectCandidateSummary(userId);
        return row == null ? emptyCandidateSummary() : row;
    }

    private DashboardProgressSummaryRow selectProgressSummary(UUID userId) {
        if (userId == null) {
            return emptyProgressSummary();
        }
        DashboardProgressSummaryRow row = dashboardDao.selectProgressSummary(userId);
        return row == null ? emptyProgressSummary() : row;
    }

    private DashboardCandidateSummaryRow emptyCandidateSummary() {
        return new DashboardCandidateSummaryRow(0, 0, 0, 0, 0, null, null);
    }

    private DashboardProgressSummaryRow emptyProgressSummary() {
        return new DashboardProgressSummaryRow(0, 0, 0, 0, 0, BigDecimal.ZERO);
    }

    private String selectVerificationStatusCode(DashboardVerificationStatusRow verification) {
        return verification == null ? "DRAFT" : verification.statusCode();
    }

    private String selectServiceStatusCode(
            DashboardVerificationStatusRow verification,
            DashboardProgressSummaryRow progressSummary,
            DashboardCandidateSummaryRow candidateSummary
    ) {
        if (progressSummary.inProgressCount() > 0) {
            return "IN_PROGRESS";
        }
        if (progressSummary.waitingResultCount() > 0) {
            return "WAITING_RESULT";
        }
        if (progressSummary.approvedCount() > 0 || progressSummary.stoppedCount() > 0) {
            return "COMPLETED";
        }
        if (hasStartableMatching(candidateSummary)) {
            return "MATCHING_READY";
        }
        if (!"VERIFIED".equals(selectVerificationStatusCode(verification))) {
            return VERIFICATION_REQUIRED;
        }
        if (hasCandidate(candidateSummary)) {
            return "MATCHING_READY";
        }
        return "MATCHING_READY";
    }

    private boolean hasStartableMatching(DashboardCandidateSummaryRow candidateSummary) {
        return candidateSummary.startableMatchedCount() > 0;
    }

    private boolean hasCandidate(DashboardCandidateSummaryRow candidateSummary) {
        return candidateSummary.policyFundCount() > 0
                || candidateSummary.supportFundCount() > 0
                || candidateSummary.subsidyCount() > 0
                || candidateSummary.finalMatchedCount() > 0
                || candidateSummary.minAmount() != null
                || candidateSummary.maxAmount() != null;
    }

    private String selectNoticeMessage(
            DashboardVerificationStatusRow verification,
            DashboardCandidateSummaryRow candidateSummary
    ) {
        if (!"VERIFIED".equals(selectVerificationStatusCode(verification))) {
            return "전자증명 검증 전 참고 결과입니다.";
        }
        if (!hasCandidate(candidateSummary)) {
            return "matching_cases 기준 진행 가능한 공고가 없습니다.";
        }
        return "matching_cases 기준 진행 가능한 공고를 표시합니다.";
    }

    private String selectCurrentStepDescription(DashboardCurrentStepRow currentStep) {
        if (currentStep.actionGuide() != null && !currentStep.actionGuide().isBlank()) {
            return currentStep.actionGuide();
        }
        return currentStep.guideMessage();
    }

    private int selectStepDisplayOrder(String stepStatusCode) {
        return "IN_PROGRESS".equals(stepStatusCode) ? 10 : 20;
    }
}

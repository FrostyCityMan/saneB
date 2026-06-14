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
    private static final String BASIC_INFO_REQUIRED = "BASIC_INFO_REQUIRED";
    private static final String BASIC_MATCHING_REVIEW_REQUIRED = "BASIC_MATCHING_REVIEW_REQUIRED";
    private static final String SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_REQUIRED";
    private static final String CONSULTATION_REQUEST_REQUIRED = "CONSULTATION_REQUEST_REQUIRED";
    private static final String FINAL_MATCHING_WAITING = "FINAL_MATCHING_WAITING";
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
                new DashboardSummaryResponse.TargetCandidateCountsResponse(
                        candidateSummary.businessTargetCount(),
                        candidateSummary.personalTargetCount(),
                        candidateSummary.familyTargetCount()
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

        DashboardCandidateSummaryRow candidateSummary = selectCandidateSummary(userId);
        if (dashboardDao.selectBasicInfoSavedCount(userId) < 1) {
            return new DashboardCurrentActionResponse(
                    BASIC_INFO_REQUIRED,
                    "기본 정보를 입력해 주세요.",
                    "사업자·개인·가족 기본정보를 저장하면 공고 조건과 비교해 진행 가능 현황을 확인합니다.",
                    "기본 정보 입력",
                    "/app/member/basic-info",
                    null,
                    5
            );
        }
        if (!hasCandidate(candidateSummary)) {
            return new DashboardCurrentActionResponse(
                    NO_ACTION,
                    "기본정보와 맞는 공고를 확인 중입니다.",
                    "저장된 기본정보 기준으로 바로 진행 가능한 공고가 아직 없습니다. 필요하면 기본정보를 수정해 주세요.",
                    "기본정보 수정",
                    "/app/member/basic-info",
                    null,
                    30
            );
        }
        if (dashboardDao.selectActiveSubscriptionCount(userId) < 1) {
            return new DashboardCurrentActionResponse(
                    BASIC_MATCHING_REVIEW_REQUIRED,
                    "현재 매칭 공고를 확인해 주세요.",
                    "저장한 기본정보와 맞는 공고를 확인한 뒤 구독과 상담을 진행합니다.",
                    "현재 매칭 공고 보기",
                    "/app/matching/basic-candidates",
                    null,
                    10
            );
        }
        if (dashboardDao.selectConsultationReservationCount(userId) < 1) {
            return new DashboardCurrentActionResponse(
                    CONSULTATION_REQUEST_REQUIRED,
                    "첫 상담을 요청해 주세요.",
                    "구독이 활성화되었습니다. 운영자가 담당자를 배정할 수 있도록 상담 요청을 남겨 주세요.",
                    "상담 요청",
                    "/app/consultations",
                    null,
                    15
            );
        }
        if (candidateSummary.finalMatchedCount() < 1) {
            return new DashboardCurrentActionResponse(
                    FINAL_MATCHING_WAITING,
                    "최종 매칭 확인 중입니다.",
                    "상담과 서류 입력 내용을 기준으로 관리자가 진행할 공고를 확인하고 있습니다.",
                    null,
                    null,
                    null,
                    20
            );
        }
        DashboardProgressSummaryRow progressSummary = selectProgressSummary(userId);
        if (progressSummary.waitingResultCount() > 0
                || progressSummary.approvedCount() > 0
                || progressSummary.stoppedCount() > 0) {
            return new DashboardCurrentActionResponse(
                    NO_ACTION,
                    "현재 처리할 진행 행동이 없습니다.",
                    "진행 중인 신청 행동이 없거나 결과 확인 상태입니다.",
                    null,
                    null,
                    null,
                    99
            );
        }

        return new DashboardCurrentActionResponse(
                NO_ACTION,
                "진행 시작을 준비 중입니다.",
                "관리자가 최종 매칭된 공고로 신청 진행을 시작하면 다음 행동이 표시됩니다.",
                null,
                null,
                null,
                90
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
        return new DashboardCandidateSummaryRow(0, 0, 0, 0, 0, 0, 0, 0, null, null);
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
        if (hasCandidate(candidateSummary)) {
            return "MATCHING_READY";
        }
        return BASIC_INFO_REQUIRED;
    }

    private boolean hasStartableMatching(DashboardCandidateSummaryRow candidateSummary) {
        return candidateSummary.startableMatchedCount() > 0;
    }

    private boolean hasCandidate(DashboardCandidateSummaryRow candidateSummary) {
        return candidateSummary.policyFundCount() > 0
                || candidateSummary.supportFundCount() > 0
                || candidateSummary.subsidyCount() > 0
                || candidateSummary.businessTargetCount() > 0
                || candidateSummary.personalTargetCount() > 0
                || candidateSummary.familyTargetCount() > 0
                || candidateSummary.finalMatchedCount() > 0
                || candidateSummary.minAmount() != null
                || candidateSummary.maxAmount() != null;
    }

    private String selectNoticeMessage(
            DashboardVerificationStatusRow verification,
            DashboardCandidateSummaryRow candidateSummary
    ) {
        if (!hasCandidate(candidateSummary)) {
            return "저장된 기본정보 기준으로 진행 가능한 공고가 아직 없습니다.";
        }
        return "기본정보 기준으로 현재 확인 가능한 공고입니다. 서류 입력 후 최종 매칭이 다시 정리됩니다.";
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

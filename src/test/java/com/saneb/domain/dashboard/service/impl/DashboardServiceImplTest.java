package com.saneb.domain.dashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dashboard.dao.DashboardDao;
import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.vo.DashboardCandidateSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardCurrentStepRow;
import com.saneb.domain.dashboard.vo.DashboardProgressSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardVerificationStatusRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private DashboardDao dashboardDao;

    private DashboardServiceImpl dashboardService;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(dashboardDao);
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "local_user",
                        "{bcrypt}hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("USER")
        );
        authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    @Test
    void selectMySummaryUsesMatchingCasesAndAnnouncementAmountRange() {
        when(dashboardDao.selectCurrentVerificationStatus(USER_ID)).thenReturn(
                new DashboardVerificationStatusRow("VERIFIED", OffsetDateTime.parse("2026-05-01T10:00:00+09:00"))
        );
        when(dashboardDao.selectCandidateSummary(USER_ID)).thenReturn(
                new DashboardCandidateSummaryRow(
                        2,
                        1,
                        1,
                        2,
                        1,
                        1,
                        1,
                        3,
                        new BigDecimal("1000000.00"),
                        new BigDecimal("5000000.00")
                )
        );
        when(dashboardDao.selectProgressSummary(USER_ID)).thenReturn(
                new DashboardProgressSummaryRow(1, 0, 0, 0, 0, BigDecimal.ZERO)
        );

        DashboardSummaryResponse response = dashboardService.selectMySummary(authentication);

        assertThat(response.serviceStatusCode()).isEqualTo("IN_PROGRESS");
        assertThat(response.candidateCounts().policyFund()).isEqualTo(2);
        assertThat(response.candidateCounts().supportFund()).isEqualTo(1);
        assertThat(response.candidateCounts().subsidy()).isEqualTo(1);
        assertThat(response.targetCandidateCounts().business()).isEqualTo(2);
        assertThat(response.targetCandidateCounts().personal()).isEqualTo(1);
        assertThat(response.targetCandidateCounts().family()).isEqualTo(1);
        assertThat(response.finalMatchedCount()).isEqualTo(3);
        assertThat(response.supportAmountRange().minAmount()).isEqualByComparingTo("1000000.00");
        assertThat(response.supportAmountRange().maxAmount()).isEqualByComparingTo("5000000.00");
        assertThat(response.supportAmountRange().basisCode()).isEqualTo("ANNOUNCEMENT_AMOUNT_RANGE");
        assertThat(response.verificationStatusCode()).isEqualTo("VERIFIED");
    }

    @Test
    void selectMySummaryReturnsEmptyStateWhenMatchingCasesDoNotExist() {
        when(dashboardDao.selectCurrentVerificationStatus(USER_ID)).thenReturn(
                new DashboardVerificationStatusRow("VERIFIED", OffsetDateTime.parse("2026-05-01T10:00:00+09:00"))
        );
        when(dashboardDao.selectCandidateSummary(USER_ID)).thenReturn(
                new DashboardCandidateSummaryRow(0, 0, 0, 0, 0, 0, 0, 0, null, null)
        );
        when(dashboardDao.selectProgressSummary(USER_ID)).thenReturn(
                new DashboardProgressSummaryRow(0, 0, 0, 0, 0, BigDecimal.ZERO)
        );

        DashboardSummaryResponse response = dashboardService.selectMySummary(authentication);

        assertThat(response.candidateCounts().policyFund()).isZero();
        assertThat(response.candidateCounts().supportFund()).isZero();
        assertThat(response.candidateCounts().subsidy()).isZero();
        assertThat(response.finalMatchedCount()).isZero();
        assertThat(response.supportAmountRange().minAmount()).isNull();
        assertThat(response.supportAmountRange().maxAmount()).isNull();
        assertThat(response.noticeMessage()).isEqualTo("저장된 기본정보 기준으로 진행 가능한 공고가 아직 없습니다.");
    }

    @Test
    void selectMyCurrentActionPrefersReadyOrInProgressStepState() {
        when(dashboardDao.selectCurrentStepDetails(USER_ID)).thenReturn(
                new DashboardCurrentStepRow(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "IN_PROGRESS",
                        "서류 제출",
                        "필수 서류를 확인해 주세요.",
                        "제출 서류를 검토하고 다음 행동을 선택해 주세요.",
                        "제출 확인",
                        LocalDate.of(2026, 6, 30)
                )
        );

        DashboardCurrentActionResponse response = dashboardService.selectMyCurrentAction(authentication);

        assertThat(response.actionCode()).isEqualTo("PROGRESS_ACTION_REQUIRED");
        assertThat(response.title()).isEqualTo("서류 제출");
        assertThat(response.description()).isEqualTo("제출 서류를 검토하고 다음 행동을 선택해 주세요.");
        assertThat(response.primaryButtonLabel()).isEqualTo("제출 확인");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(response.displayOrder()).isEqualTo(10);
    }

    @Test
    void selectMyCurrentActionReturnsBasicInfoRequiredWhenNoStepExists() {
        when(dashboardDao.selectCurrentStepDetails(USER_ID)).thenReturn(null);
        when(dashboardDao.selectCandidateSummary(USER_ID)).thenReturn(
                new DashboardCandidateSummaryRow(0, 0, 0, 0, 0, 0, 0, 0, null, null)
        );
        when(dashboardDao.selectBasicInfoSavedCount(USER_ID)).thenReturn(0L);

        DashboardCurrentActionResponse response = dashboardService.selectMyCurrentAction(authentication);

        assertThat(response.actionCode()).isEqualTo("BASIC_INFO_REQUIRED");
        assertThat(response.primaryButtonLabel()).isEqualTo("기본 정보 입력");
        assertThat(response.route()).isEqualTo("/app/member/basic-info");
    }

    @Test
    void selectMyCurrentActionRequiresBasicMatchingReviewWhenBasicCandidateExists() {
        when(dashboardDao.selectCurrentStepDetails(USER_ID)).thenReturn(null);
        when(dashboardDao.selectCandidateSummary(USER_ID)).thenReturn(
                new DashboardCandidateSummaryRow(0, 0, 0, 0, 0, 0, 1, 1, null, null)
        );
        when(dashboardDao.selectBasicInfoSavedCount(USER_ID)).thenReturn(1L);
        when(dashboardDao.selectActiveSubscriptionCount(USER_ID)).thenReturn(0L);

        DashboardCurrentActionResponse response = dashboardService.selectMyCurrentAction(authentication);

        assertThat(response.actionCode()).isEqualTo("BASIC_MATCHING_REVIEW_REQUIRED");
        assertThat(response.primaryButtonLabel()).isEqualTo("현재 매칭 공고 보기");
        assertThat(response.route()).isEqualTo("/app/matching/basic-candidates");
    }

    @Test
    void selectMyCurrentActionRequiresConsultationWhenSubscriptionIsActive() {
        when(dashboardDao.selectCurrentStepDetails(USER_ID)).thenReturn(null);
        when(dashboardDao.selectCandidateSummary(USER_ID)).thenReturn(
                new DashboardCandidateSummaryRow(0, 0, 0, 1, 0, 0, 1, 0, null, null)
        );
        when(dashboardDao.selectBasicInfoSavedCount(USER_ID)).thenReturn(1L);
        when(dashboardDao.selectActiveSubscriptionCount(USER_ID)).thenReturn(1L);
        when(dashboardDao.selectConsultationReservationCount(USER_ID)).thenReturn(0L);

        DashboardCurrentActionResponse response = dashboardService.selectMyCurrentAction(authentication);

        assertThat(response.actionCode()).isEqualTo("CONSULTATION_REQUEST_REQUIRED");
        assertThat(response.primaryButtonLabel()).isEqualTo("상담 요청");
        assertThat(response.route()).isEqualTo("/app/consultations");
    }

    @Test
    void selectMyCurrentActionWaitsForFinalMatchingAfterConsultationRequest() {
        when(dashboardDao.selectCurrentStepDetails(USER_ID)).thenReturn(null);
        when(dashboardDao.selectCandidateSummary(USER_ID)).thenReturn(
                new DashboardCandidateSummaryRow(0, 0, 0, 1, 0, 0, 1, 0, null, null)
        );
        when(dashboardDao.selectBasicInfoSavedCount(USER_ID)).thenReturn(1L);
        when(dashboardDao.selectActiveSubscriptionCount(USER_ID)).thenReturn(1L);
        when(dashboardDao.selectConsultationReservationCount(USER_ID)).thenReturn(1L);

        DashboardCurrentActionResponse response = dashboardService.selectMyCurrentAction(authentication);

        assertThat(response.actionCode()).isEqualTo("FINAL_MATCHING_WAITING");
        assertThat(response.route()).isNull();
    }

    @Test
    void selectMyProgressSummaryUsesApplicationProgressesOnly() {
        when(dashboardDao.selectProgressSummary(USER_ID)).thenReturn(
                new DashboardProgressSummaryRow(
                        2,
                        1,
                        3,
                        1,
                        1,
                        new BigDecimal("7000000.00")
                )
        );

        DashboardProgressSummaryResponse response = dashboardService.selectMyProgressSummary(authentication);

        assertThat(response.inProgressCount()).isEqualTo(2);
        assertThat(response.waitingResultCount()).isEqualTo(1);
        assertThat(response.approvedCount()).isEqualTo(3);
        assertThat(response.supplementRequestedCount()).isEqualTo(1);
        assertThat(response.stoppedCount()).isEqualTo(1);
        assertThat(response.totalReceivedAmount()).isEqualByComparingTo("7000000.00");
    }

    @Test
    void selectMyReverificationStatusUsesExpiredVerificationState() {
        OffsetDateTime verifiedAt = OffsetDateTime.parse("2026-05-01T10:00:00+09:00");
        when(dashboardDao.selectCurrentVerificationStatus(USER_ID)).thenReturn(
                new DashboardVerificationStatusRow("EXPIRED", verifiedAt)
        );

        DashboardReverificationStatusResponse response = dashboardService.selectMyReverificationStatus(authentication);

        assertThat(response.required()).isTrue();
        assertThat(response.lastVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(response.reasonCode()).isEqualTo("VERIFICATION_EXPIRED");
        assertThat(response.requiredItems()).containsExactly("BUSINESS_STATUS", "TAX_STATUS", "FINANCIAL_STATUS");
    }
}

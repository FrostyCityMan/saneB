package com.saneb.domain.dashboard.service.impl;

import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Override
    public DashboardSummaryResponse selectMySummary(Authentication authentication) {
        return new DashboardSummaryResponse(
                "VERIFICATION_REQUIRED",
                new DashboardSummaryResponse.CandidateCountsResponse(0, 0, 0),
                0,
                new DashboardSummaryResponse.SupportAmountRangeResponse(null, null, "ANNOUNCEMENT_AMOUNT_RANGE"),
                "DRAFT",
                "전자증명 검증 전 참고 결과입니다."
        );
    }

    @Override
    public DashboardCurrentActionResponse selectMyCurrentAction(Authentication authentication) {
        return new DashboardCurrentActionResponse(
                "VERIFICATION_DOCUMENT_REQUIRED",
                "전자증명 검증이 필요합니다.",
                "최종 매칭 전 파트너 검증과 필수 서류 확인이 필요합니다.",
                "검증 진행하기",
                "/app/member/verifications/current",
                null,
                5
        );
    }

    @Override
    public DashboardProgressSummaryResponse selectMyProgressSummary(Authentication authentication) {
        return new DashboardProgressSummaryResponse(0, 0, 0, 0, 0, BigDecimal.ZERO);
    }

    @Override
    public DashboardReverificationStatusResponse selectMyReverificationStatus(Authentication authentication) {
        return new DashboardReverificationStatusResponse(false, null, null, List.of());
    }
}

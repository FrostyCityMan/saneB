package com.saneb.domain.dashboard.service;

import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import org.springframework.security.core.Authentication;

public interface DashboardService {

    DashboardSummaryResponse selectMySummary(Authentication authentication);

    DashboardCurrentActionResponse selectMyCurrentAction(Authentication authentication);

    DashboardProgressSummaryResponse selectMyProgressSummary(Authentication authentication);

    DashboardReverificationStatusResponse selectMyReverificationStatus(Authentication authentication);
}

package com.saneb.domain.dashboard.dao;

import com.saneb.domain.dashboard.vo.DashboardCandidateSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardCurrentStepRow;
import com.saneb.domain.dashboard.vo.DashboardProgressSummaryRow;
import com.saneb.domain.dashboard.vo.DashboardVerificationStatusRow;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardDao {

    UUID selectUserIdByLoginId(@Param("loginId") String loginId);

    DashboardVerificationStatusRow selectCurrentVerificationStatus(@Param("userId") UUID userId);

    DashboardCandidateSummaryRow selectCandidateSummary(@Param("userId") UUID userId);

    DashboardProgressSummaryRow selectProgressSummary(@Param("userId") UUID userId);

    DashboardCurrentStepRow selectCurrentStepDetails(@Param("userId") UUID userId);
}

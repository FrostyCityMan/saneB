package com.saneb.domain.operatordashboard.service.impl;

import com.saneb.domain.operatordashboard.dao.OperatorDashboardDao;
import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse;
import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse.AnnouncementWorkResponse;
import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse.ApplicationProgressWorkResponse;
import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse.MatchingWorkResponse;
import com.saneb.domain.operatordashboard.service.OperatorDashboardService;
import com.saneb.domain.operatordashboard.vo.OperatorAnnouncementWorkRow;
import com.saneb.domain.operatordashboard.vo.OperatorApplicationProgressWorkRow;
import com.saneb.domain.operatordashboard.vo.OperatorMatchingWorkRow;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class OperatorDashboardServiceImpl implements OperatorDashboardService {

    private final OperatorDashboardDao operatorDashboardDao;

    public OperatorDashboardServiceImpl(OperatorDashboardDao operatorDashboardDao) {
        this.operatorDashboardDao = operatorDashboardDao;
    }

    @Override
    public OperatorDashboardSummaryResponse selectSummary() {
        OperatorAnnouncementWorkRow announcementWork = nullToEmpty(operatorDashboardDao.selectAnnouncementWork());
        OperatorMatchingWorkRow matchingWork = nullToEmpty(operatorDashboardDao.selectMatchingWork());
        OperatorApplicationProgressWorkRow progressWork =
                nullToEmpty(operatorDashboardDao.selectApplicationProgressWork());

        return new OperatorDashboardSummaryResponse(
                new AnnouncementWorkResponse(
                        announcementWork.draftCount(),
                        announcementWork.requestedCount(),
                        announcementWork.openAnnouncementCount(),
                        announcementWork.pausedAnnouncementCount(),
                        announcementWork.closedAnnouncementCount()
                ),
                new MatchingWorkResponse(
                        matchingWork.matchedCount(),
                        matchingWork.reviewRequiredCount(),
                        matchingWork.blockedCount(),
                        matchingWork.progressedCount()
                ),
                new ApplicationProgressWorkResponse(
                        progressWork.readyCount(),
                        progressWork.inProgressCount(),
                        progressWork.waitingResultCount(),
                        progressWork.approvedCount(),
                        progressWork.supplementRequestedCount(),
                        progressWork.stoppedCount(),
                        progressWork.totalReceivedAmount() == null
                                ? BigDecimal.ZERO
                                : progressWork.totalReceivedAmount()
                )
        );
    }

    private OperatorAnnouncementWorkRow nullToEmpty(OperatorAnnouncementWorkRow row) {
        return row == null ? new OperatorAnnouncementWorkRow(0, 0, 0, 0, 0) : row;
    }

    private OperatorMatchingWorkRow nullToEmpty(OperatorMatchingWorkRow row) {
        return row == null ? new OperatorMatchingWorkRow(0, 0, 0, 0) : row;
    }

    private OperatorApplicationProgressWorkRow nullToEmpty(OperatorApplicationProgressWorkRow row) {
        return row == null ? new OperatorApplicationProgressWorkRow(
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO
        ) : row;
    }
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: OperatorDashboardServiceImpl.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 객체를 생성합니다.
     *
     * @param operatorDashboardDao 입력 값
     */
    public OperatorDashboardServiceImpl(OperatorDashboardDao operatorDashboardDao) {
        this.operatorDashboardDao = operatorDashboardDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private OperatorAnnouncementWorkRow nullToEmpty(OperatorAnnouncementWorkRow row) {
        return row == null ? new OperatorAnnouncementWorkRow(0, 0, 0, 0, 0) : row;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private OperatorMatchingWorkRow nullToEmpty(OperatorMatchingWorkRow row) {
        return row == null ? new OperatorMatchingWorkRow(0, 0, 0, 0) : row;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
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

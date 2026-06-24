/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: OperatorDashboardServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operatordashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.saneb.domain.operatordashboard.dao.OperatorDashboardDao;
import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse;
import com.saneb.domain.operatordashboard.vo.OperatorAnnouncementWorkRow;
import com.saneb.domain.operatordashboard.vo.OperatorApplicationProgressWorkRow;
import com.saneb.domain.operatordashboard.vo.OperatorMatchingWorkRow;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorDashboardServiceImplTest {

    @Mock
    private OperatorDashboardDao operatorDashboardDao;

    private OperatorDashboardServiceImpl operatorDashboardService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        operatorDashboardService = new OperatorDashboardServiceImpl(operatorDashboardDao);
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectSummaryMapsOperatorWorkCountsWithoutSensitiveValues() {
        when(operatorDashboardDao.selectAnnouncementWork()).thenReturn(
                new OperatorAnnouncementWorkRow(3, 2, 8, 1, 6)
        );
        when(operatorDashboardDao.selectMatchingWork()).thenReturn(
                new OperatorMatchingWorkRow(7, 2, 1, 4)
        );
        when(operatorDashboardDao.selectApplicationProgressWork()).thenReturn(
                new OperatorApplicationProgressWorkRow(
                        1,
                        2,
                        3,
                        4,
                        0,
                        1,
                        new BigDecimal("5000000.00")
                )
        );

        OperatorDashboardSummaryResponse response = operatorDashboardService.selectSummary();

        assertThat(response.announcementWork().requestedCount()).isEqualTo(2);
        assertThat(response.matchingWork().matchedCount()).isEqualTo(7);
        assertThat(response.applicationProgressWork().waitingResultCount()).isEqualTo(3);
        assertThat(response.applicationProgressWork().totalReceivedAmount()).isEqualByComparingTo("5000000.00");
    }
}

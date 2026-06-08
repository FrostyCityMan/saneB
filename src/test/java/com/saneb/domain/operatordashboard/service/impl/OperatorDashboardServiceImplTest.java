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

    @BeforeEach
    void setUp() {
        operatorDashboardService = new OperatorDashboardServiceImpl(operatorDashboardDao);
    }

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

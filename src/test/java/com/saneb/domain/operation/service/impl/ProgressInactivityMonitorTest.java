/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ProgressInactivityMonitorTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.domain.operation.dao.OperationDao;
import com.saneb.domain.operation.vo.NotificationMessageInsertCommand;
import com.saneb.domain.operation.vo.StatusRefreshTargetUserRow;
import com.saneb.domain.operation.vo.StalledApplicationProgressRow;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressInactivityMonitorTest {

    private static final UUID PROGRESS_ID = UUID.fromString("92000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID STEP_ID = UUID.fromString("92000000-0000-0000-0000-000000000003");
    private static final int BATCH_SIZE = 10;

    @Mock
    private OperationDao operationDao;

    private ProgressInactivityMonitor monitor;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        monitor = new ProgressInactivityMonitor(operationDao, BATCH_SIZE);
    }

    /**
     * 대상 데이터를 분류합니다.
     */
    @Test
    void classifyInactiveProgressesUsesUserFacingReminderRules() {
        StalledApplicationProgressRow progress = progressRow();
        when(operationDao.selectStalledApplicationProgressList(anyString(), any(), eq(BATCH_SIZE)))
                .thenAnswer(invocation -> List.of(progress));
        when(operationDao.selectDeadlineReminderProgressList(eq("DEADLINE_D_MINUS_2"), any(), eq(BATCH_SIZE)))
                .thenReturn(List.of(progress));
        when(operationDao.selectStatusRefreshTargetUserList(any(), any(), eq(BATCH_SIZE)))
                .thenReturn(List.of(new StatusRefreshTargetUserRow(USER_ID)));

        monitor.classifyInactiveProgresses();

        ArgumentCaptor<OffsetDateTime> reGuideThresholdCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(operationDao).selectStalledApplicationProgressList(
                eq("RE_GUIDE"),
                reGuideThresholdCaptor.capture(),
                eq(BATCH_SIZE)
        );
        long thresholdGapSeconds = Math.abs(Duration.between(
                reGuideThresholdCaptor.getValue(),
                OffsetDateTime.now().minusHours(48)
        ).toSeconds());
        assertThat(thresholdGapSeconds).isLessThan(5);

        verify(operationDao).selectDeadlineReminderProgressList(
                eq("DEADLINE_D_MINUS_2"),
                eq(LocalDate.now().plusDays(2)),
                eq(BATCH_SIZE)
        );
        verify(operationDao).selectStatusRefreshTargetUserList(any(), any(), eq(BATCH_SIZE));

        ArgumentCaptor<NotificationMessageInsertCommand> notificationCaptor =
                ArgumentCaptor.forClass(NotificationMessageInsertCommand.class);
        verify(operationDao, org.mockito.Mockito.times(6)).insertNotificationMessage(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(NotificationMessageInsertCommand::title)
                .contains(
                        "공고 신청 진행 안내",
                        "공고 신청 재안내",
                        "공고 마감 2일 전 안내",
                        "장기 미진행 분류",
                        "TM 재접촉 대상",
                        "정보 재확인이 필요합니다"
                );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private StalledApplicationProgressRow progressRow() {
        return new StalledApplicationProgressRow(
                PROGRESS_ID,
                USER_ID,
                STEP_ID,
                "IN_PROGRESS",
                OffsetDateTime.now().minusDays(15)
        );
    }
}

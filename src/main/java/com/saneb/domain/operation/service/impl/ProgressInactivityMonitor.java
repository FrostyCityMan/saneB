/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ProgressInactivityMonitor.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.operation.service.impl;

import com.saneb.domain.operation.dao.OperationDao;
import com.saneb.domain.operation.vo.NotificationDeliveryLogCommand;
import com.saneb.domain.operation.vo.NotificationMessageInsertCommand;
import com.saneb.domain.operation.vo.OperationTaskInsertCommand;
import com.saneb.domain.operation.vo.ProgressReminderInsertCommand;
import com.saneb.domain.operation.vo.StatusRefreshTargetUserRow;
import com.saneb.domain.operation.vo.StalledApplicationProgressRow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "saneb.operation.inactivity", name = "enabled", havingValue = "true")
public class ProgressInactivityMonitor {

    private static final Logger log = LoggerFactory.getLogger(ProgressInactivityMonitor.class);
    private static final String RESOURCE_TYPE = "APPLICATION_PROGRESS";

    private final OperationDao operationDao;
    private final int batchSize;

    /**
     * 객체를 생성합니다.
     *
     * @param operationDao 입력 값
     *
     * @param batchSize 입력 값
     */
    public ProgressInactivityMonitor(
            OperationDao operationDao,
            @Value("$" + "{saneb.operation.inactivity.batch-size:50}") int batchSize
    ) {
        this.operationDao = operationDao;
        this.batchSize = Math.max(1, batchSize);
    }

    /**
     * 대상 데이터를 분류합니다.
     */
    @Scheduled(
            initialDelayString = "$" + "{saneb.operation.inactivity.initial-delay-ms:60000}",
            fixedDelayString = "$" + "{saneb.operation.inactivity.fixed-delay-ms:3600000}"
    )
    /**
     * 대상 데이터를 분류합니다.
     */
    @Transactional
    public void classifyInactiveProgresses() {
        OffsetDateTime now = OffsetDateTime.now();
        int handledCount = 0;
        handledCount += handleReminder("FIRST_REMINDER", now.minusHours(24), 1, "공고 신청 진행 안내", "아직 완료되지 않은 공고 신청 단계가 있습니다.", false);
        handledCount += handleReminder("RE_GUIDE", now.minusHours(48), 2, "공고 신청 재안내", "신청 단계가 48시간 이상 멈춰 있습니다. 필요한 항목을 확인해 주세요.", false);
        handledCount += handleDeadlineReminder(LocalDate.now().plusDays(2));
        handledCount += handleReminder("LONG_STALLED", now.minusDays(7), 3, "장기 미진행 분류", "신청 단계가 7일 이상 멈춰 있어 운영 확인 대상으로 분류되었습니다.", true);
        handledCount += handleReminder("TM_RECONTACT", now.minusDays(14), 4, "TM 재접촉 대상", "신청 단계가 14일 이상 멈춰 있어 재접촉 대상으로 분류되었습니다.", true);
        handledCount += handleStatusRefreshReminder(now.minusMonths(6), now.minusDays(30));
        if (handledCount > 0) {
            log.info("Classified inactive application progresses. handledCount={}", handledCount);
        }
    }

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param reminderTypeCode 입력 값
     *
     * @param thresholdAt 입력 값
     *
     * @param attemptNo 입력 값
     *
     * @param title 입력 값
     *
     * @param body 입력 값
     *
     * @param createOperationTask 입력 값
     *
     * @return 처리 결과
     */
    private int handleReminder(
            String reminderTypeCode,
            OffsetDateTime thresholdAt,
            int attemptNo,
            String title,
            String body,
            boolean createOperationTask
    ) {
        List<StalledApplicationProgressRow> rows = operationDao.selectStalledApplicationProgressList(
                reminderTypeCode,
                thresholdAt,
                batchSize
        );
        OffsetDateTime now = OffsetDateTime.now();
        for (StalledApplicationProgressRow row : rows) {
            insertNotification(row, title, body);
            if (createOperationTask) {
                insertOperationTaskIfMissing(row, reminderTypeCode, title, now);
            }
            operationDao.insertProgressReminderLog(new ProgressReminderInsertCommand(
                    UUID.randomUUID(),
                    row.progressId(),
                    row.currentStepId(),
                    reminderTypeCode,
                    attemptNo,
                    now,
                    now,
                    "SUCCESS"
            ));
        }
        return rows.size();
    }

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param targetDate 입력 값
     *
     * @return 처리 결과
     */
    private int handleDeadlineReminder(LocalDate targetDate) {
        String reminderTypeCode = "DEADLINE_D_MINUS_2";
        List<StalledApplicationProgressRow> rows = operationDao.selectDeadlineReminderProgressList(
                reminderTypeCode,
                targetDate,
                batchSize
        );
        OffsetDateTime now = OffsetDateTime.now();
        for (StalledApplicationProgressRow row : rows) {
            insertNotification(
                    row,
                    "공고 마감 2일 전 안내",
                    "진행 중인 공고의 접수 마감이 2일 남았습니다. 필요한 행동을 확인해 주세요."
            );
            operationDao.insertProgressReminderLog(new ProgressReminderInsertCommand(
                    UUID.randomUUID(),
                    row.progressId(),
                    row.currentStepId(),
                    reminderTypeCode,
                    5,
                    now,
                    now,
                    "SUCCESS"
            ));
        }
        return rows.size();
    }

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param thresholdAt 입력 값
     *
     * @param recentSince 입력 값
     *
     * @return 처리 결과
     */
    private int handleStatusRefreshReminder(OffsetDateTime thresholdAt, OffsetDateTime recentSince) {
        List<StatusRefreshTargetUserRow> rows = operationDao.selectStatusRefreshTargetUserList(
                thresholdAt,
                recentSince,
                batchSize
        );
        for (StatusRefreshTargetUserRow row : rows) {
            insertUserNotification(
                    row.userId(),
                    "정보 재확인이 필요합니다",
                    "최근 정보 확인일 기준 6개월이 지났습니다. 정확한 매칭을 위해 사업 상태, 세금 상태, 금융 상태와 가족 정보를 다시 확인해 주세요.",
                    "GENERAL",
                    null,
                    "{\"source\":\"STATUS_REFRESH_MONITOR\",\"periodMonths\":6}"
            );
        }
        return rows.size();
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param row 입력 값
     *
     * @param title 입력 값
     *
     * @param body 입력 값
     */
    private void insertNotification(StalledApplicationProgressRow row, String title, String body) {
        insertUserNotification(
                row.memberUserId(),
                title,
                body,
                RESOURCE_TYPE,
                row.progressId(),
                "{\"source\":\"INACTIVITY_MONITOR\"}"
        );
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param recipientUserId 입력 값
     *
     * @param title 입력 값
     *
     * @param body 입력 값
     *
     * @param resourceType 입력 값
     *
     * @param resourceId 입력 값
     *
     * @param metadataJson 입력 값
     */
    private void insertUserNotification(
            UUID recipientUserId,
            String title,
            String body,
            String resourceType,
            UUID resourceId,
            String metadataJson
    ) {
        UUID notificationId = UUID.randomUUID();
        operationDao.insertNotificationMessage(new NotificationMessageInsertCommand(
                notificationId,
                recipientUserId,
                null,
                "IN_APP",
                title,
                body,
                "SENT",
                resourceType,
                resourceId,
                null
        ));
        operationDao.insertNotificationDeliveryLog(new NotificationDeliveryLogCommand(
                UUID.randomUUID(),
                notificationId,
                "IN_APP",
                "INTERNAL",
                "SUCCESS",
                1,
                null,
                null,
                null,
                metadataJson
        ));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param row 입력 값
     *
     * @param reminderTypeCode 입력 값
     *
     * @param title 입력 값
     *
     * @param now 입력 값
     */
    private void insertOperationTaskIfMissing(
            StalledApplicationProgressRow row,
            String reminderTypeCode,
            String title,
            OffsetDateTime now
    ) {
        String taskTypeCode = "TM_RECONTACT".equals(reminderTypeCode) ? "RECONTACT" : "DELAYED_PROGRESS";
        if (operationDao.selectOpenOperationTaskCount(taskTypeCode, RESOURCE_TYPE, row.progressId()) > 0) {
            return;
        }
        operationDao.insertOperationTask(new OperationTaskInsertCommand(
                UUID.randomUUID(),
                taskTypeCode,
                "TM_RECONTACT".equals(reminderTypeCode) ? "HIGH" : "NORMAL",
                title,
                "사용자 행동이 장기간 진행되지 않아 운영 확인이 필요합니다.",
                RESOURCE_TYPE,
                row.progressId(),
                now.plusDays("TM_RECONTACT".equals(reminderTypeCode) ? 1 : 2),
                null
        ));
    }
}

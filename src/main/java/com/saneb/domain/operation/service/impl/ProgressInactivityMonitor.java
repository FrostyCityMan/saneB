package com.saneb.domain.operation.service.impl;

import com.saneb.domain.operation.dao.OperationDao;
import com.saneb.domain.operation.vo.NotificationDeliveryLogCommand;
import com.saneb.domain.operation.vo.NotificationMessageInsertCommand;
import com.saneb.domain.operation.vo.OperationTaskInsertCommand;
import com.saneb.domain.operation.vo.ProgressReminderInsertCommand;
import com.saneb.domain.operation.vo.StalledApplicationProgressRow;
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

    public ProgressInactivityMonitor(
            OperationDao operationDao,
            @Value("$" + "{saneb.operation.inactivity.batch-size:50}") int batchSize
    ) {
        this.operationDao = operationDao;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(
            initialDelayString = "$" + "{saneb.operation.inactivity.initial-delay-ms:60000}",
            fixedDelayString = "$" + "{saneb.operation.inactivity.fixed-delay-ms:3600000}"
    )
    @Transactional
    public void classifyInactiveProgresses() {
        OffsetDateTime now = OffsetDateTime.now();
        int handledCount = 0;
        handledCount += handleReminder("FIRST_REMINDER", now.minusHours(24), 1, "공고 신청 진행 안내", "아직 완료되지 않은 공고 신청 단계가 있습니다.", false);
        handledCount += handleReminder("RE_GUIDE", now.minusHours(72), 2, "공고 신청 재안내", "신청 단계가 72시간 이상 멈춰 있습니다. 필요한 항목을 확인해 주세요.", false);
        handledCount += handleReminder("LONG_STALLED", now.minusDays(7), 3, "장기 미진행 분류", "신청 단계가 7일 이상 멈춰 있어 운영 확인 대상으로 분류되었습니다.", true);
        handledCount += handleReminder("TM_RECONTACT", now.minusDays(14), 4, "TM 재접촉 대상", "신청 단계가 14일 이상 멈춰 있어 재접촉 대상으로 분류되었습니다.", true);
        if (handledCount > 0) {
            log.info("Classified inactive application progresses. handledCount={}", handledCount);
        }
    }

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

    private void insertNotification(StalledApplicationProgressRow row, String title, String body) {
        UUID notificationId = UUID.randomUUID();
        operationDao.insertNotificationMessage(new NotificationMessageInsertCommand(
                notificationId,
                row.memberUserId(),
                null,
                "IN_APP",
                title,
                body,
                "SENT",
                RESOURCE_TYPE,
                row.progressId(),
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
                "{\"source\":\"INACTIVITY_MONITOR\"}"
        ));
    }

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

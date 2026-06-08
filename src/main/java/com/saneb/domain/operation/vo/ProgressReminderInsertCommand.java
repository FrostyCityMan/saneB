package com.saneb.domain.operation.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProgressReminderInsertCommand(
        UUID reminderLogId,
        UUID progressId,
        UUID stepId,
        String reminderTypeCode,
        int attemptNo,
        OffsetDateTime scheduledAt,
        OffsetDateTime sentAt,
        String resultCode
) {
}

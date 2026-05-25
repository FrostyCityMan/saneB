package com.saneb.domain.applicationprogress.vo;

import java.time.LocalDate;
import java.util.UUID;

public record ProgressReceiptCommand(
        UUID progressId,
        String receiptNo,
        LocalDate receiptDate,
        UUID actorUserId
) {
}

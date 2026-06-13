package com.saneb.domain.applicationprogress.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationProgressRow(
        UUID progressId,
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        String progressCode,
        String matchingCaseCode,
        String announcementCode,
        String memberUserCode,
        UUID currentStepId,
        String statusCode,
        String receiptNo,
        LocalDate receiptDate,
        String resultCode,
        String resultNote,
        LocalDate resultDate,
        BigDecimal receivedAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

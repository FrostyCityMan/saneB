package com.saneb.domain.applicationprogress.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationProgressSummaryResponse(
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
        LocalDate resultDate,
        BigDecimal receivedAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

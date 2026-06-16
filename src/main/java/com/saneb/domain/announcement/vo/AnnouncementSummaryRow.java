package com.saneb.domain.announcement.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementSummaryRow(
        UUID announcementId,
        String announcementCode,
        String targetTypeCode,
        String title,
        String agencyName,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String manualStatusCode,
        String approvalStatusCode,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String receptionTypeCode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

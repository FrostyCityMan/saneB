package com.saneb.domain.announcement.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AnnouncementDetailsRow(
        UUID announcementId,
        String targetTypeCode,
        String title,
        String agencyName,
        String summary,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String manualStatusCode,
        String approvalStatusCode,
        String incomeJudgementCode,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

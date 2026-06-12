package com.saneb.domain.matching.dto;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;

public record MatchingCaseSummaryResponse(
        UUID matchingCaseId,
        UUID announcementId,
        UUID memberUserId,
        UUID verificationId,
        String statusCode,
        String blockedReasonCode,
        OffsetDateTime matchedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String announcementTitle,
        String agencyName,
        String targetTypeCode,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String memberLoginId,
        String memberName,
        boolean progressCreated
) {
}

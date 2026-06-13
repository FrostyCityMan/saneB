package com.saneb.domain.matching.vo;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;

public record MatchingCaseRow(
        UUID matchingCaseId,
        String matchingCaseCode,
        UUID announcementId,
        String announcementCode,
        UUID memberUserId,
        String memberUserCode,
        UUID verificationId,
        String verificationCode,
        String statusCode,
        String blockedReasonCode,
        OffsetDateTime matchedAt,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
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
        Boolean progressCreated
) {
}

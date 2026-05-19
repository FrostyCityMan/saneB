package com.saneb.domain.announcement.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnnouncementSaveCommand(
        UUID id,
        String targetTypeCode,
        String title,
        String agencyName,
        String summary,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String incomeJudgementCode,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        UUID actorUserId
) {
}

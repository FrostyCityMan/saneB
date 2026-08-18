package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record AnnouncementSourceReclassificationRunPreviewRequest(
        @NotNull UUID ruleReleaseId,
        @Size(max = 40) String providerCode,
        LocalDate collectedFrom,
        LocalDate collectedTo,
        Boolean includeLinkedAnnouncements,
        @NotNull @Min(1) @Max(100000) Integer maximumCount,
        @NotNull @Min(1) @Max(100) Integer batchSize,
        @NotBlank @Size(max = 1000) String changeReason
) {

    @AssertTrue(message = "수집 종료일은 수집 시작일보다 빠를 수 없습니다.")
    public boolean isCollectedPeriodValid() {
        return collectedFrom == null || collectedTo == null || !collectedFrom.isAfter(collectedTo);
    }
}

package com.saneb.domain.announcementsource.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AnnouncementSourceExclusionSummaryResponse(
        long uniqueExcludedCount, long unknownPurposeCount,
        List<CountItem> byProvider, List<CountItem> byLatestReason,
        String countingBasis, String scope, OffsetDateTime asOf
) {
    public record CountItem(String code, long count) { }
}

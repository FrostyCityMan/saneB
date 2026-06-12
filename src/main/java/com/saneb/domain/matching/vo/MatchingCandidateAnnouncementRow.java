package com.saneb.domain.matching.vo;

import java.util.UUID;

public record MatchingCandidateAnnouncementRow(
        UUID announcementId,
        Long checkedConditionCount,
        Long matchedConditionCount
) {
}

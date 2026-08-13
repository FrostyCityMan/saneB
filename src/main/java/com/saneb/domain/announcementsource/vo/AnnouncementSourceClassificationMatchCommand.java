package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceClassificationMatchCommand(
        UUID id,
        UUID evaluationId,
        UUID ruleReleaseId,
        String ruleCode,
        String matchedRuleTerm,
        String matchedText,
        String locationCode,
        Integer startOffset,
        Integer endOffset,
        String appliedActionCode
) {
}

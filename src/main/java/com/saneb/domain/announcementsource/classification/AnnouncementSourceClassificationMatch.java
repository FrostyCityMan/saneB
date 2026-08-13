package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.AppliedActionCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchLocationCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;

/**
 * 판정에 사용됐거나 기관명 보호로 무시된 원문 일치 근거입니다.
 */
public record AnnouncementSourceClassificationMatch(
        String ruleCode,
        String groupCode,
        RuleGroupKindCode groupKindCode,
        String canonicalKeyword,
        String matchedRuleTerm,
        String matchedTerm,
        MatchLocationCode locationCode,
        int startOffset,
        int endOffset,
        AppliedActionCode appliedActionCode,
        boolean maskedByProtectedMetadata
) {
}

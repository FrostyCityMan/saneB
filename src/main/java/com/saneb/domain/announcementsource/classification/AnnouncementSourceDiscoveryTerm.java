package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import java.util.Objects;

/**
 * 외부 제공자 검색에만 사용하는 최소 대표 검색어입니다.
 */
public record AnnouncementSourceDiscoveryTerm(
        String groupCode,
        RuleGroupKindCode groupKindCode,
        String termText,
        int discoveryOrder
) {

    public AnnouncementSourceDiscoveryTerm {
        Objects.requireNonNull(groupCode, "groupCode is required");
        Objects.requireNonNull(groupKindCode, "groupKindCode is required");
        Objects.requireNonNull(termText, "termText is required");
        if (termText.isBlank()) {
            throw new IllegalArgumentException("termText must not be blank");
        }
        if (discoveryOrder < 0) {
            throw new IllegalArgumentException("discoveryOrder must not be negative");
        }
    }
}

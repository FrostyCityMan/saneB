package com.saneb.domain.announcementsource.classification;

import java.util.List;

/**
 * 한 번의 수집 실행에서 고정해서 사용하는 규칙 release입니다.
 */
public record AnnouncementSourceClassificationRuleSet(
        String releaseCode,
        List<AnnouncementSourceClassificationRule> rules
) {

    public AnnouncementSourceClassificationRuleSet {
        if (releaseCode == null || releaseCode.isBlank()) {
            throw new IllegalArgumentException("releaseCode is required");
        }
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}

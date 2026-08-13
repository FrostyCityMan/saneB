package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import java.util.List;
import java.util.Objects;

/**
 * 분류 그룹에 속한 키워드 규칙입니다.
 */
public record AnnouncementSourceClassificationRule(
        String ruleCode,
        String groupCode,
        RuleGroupKindCode groupKindCode,
        String canonicalKeyword,
        StrengthCode strengthCode,
        TargetCategoryCode targetCategoryCode,
        SupportTypeCode supportTypeCode,
        List<AnnouncementSourceClassificationTerm> terms,
        boolean enabled
) {

    public AnnouncementSourceClassificationRule {
        if (ruleCode == null || ruleCode.isBlank()) {
            throw new IllegalArgumentException("ruleCode is required");
        }
        if (groupCode == null || groupCode.isBlank()) {
            throw new IllegalArgumentException("groupCode is required");
        }
        if (canonicalKeyword == null || canonicalKeyword.isBlank()) {
            throw new IllegalArgumentException("canonicalKeyword is required");
        }
        Objects.requireNonNull(groupKindCode, "groupKindCode is required");
        Objects.requireNonNull(strengthCode, "strengthCode is required");
        terms = terms == null ? List.of() : List.copyOf(terms);
        boolean hasRequiredTerm = groupKindCode == RuleGroupKindCode.PROTECTED_METADATA
                ? terms.stream().anyMatch(AnnouncementSourceClassificationTerm::enabled)
                : terms.stream().anyMatch(term -> term.enabled() && term.classificationTerm());
        if (!hasRequiredTerm) {
            throw new IllegalArgumentException(
                    groupKindCode == RuleGroupKindCode.PROTECTED_METADATA
                            ? "at least one enabled protection term is required"
                            : "at least one enabled classification term is required"
            );
        }
        if (groupKindCode == RuleGroupKindCode.TARGET && targetCategoryCode == null) {
            throw new IllegalArgumentException("targetCategoryCode is required for TARGET rules");
        }
        if (groupKindCode != RuleGroupKindCode.TARGET && targetCategoryCode != null) {
            throw new IllegalArgumentException("targetCategoryCode is only allowed for TARGET rules");
        }
        if (groupKindCode == RuleGroupKindCode.SUPPORT_TYPE && supportTypeCode == null) {
            throw new IllegalArgumentException("supportTypeCode is required for SUPPORT_TYPE rules");
        }
        if (groupKindCode != RuleGroupKindCode.SUPPORT_TYPE && supportTypeCode != null) {
            throw new IllegalArgumentException("supportTypeCode is only allowed for SUPPORT_TYPE rules");
        }
    }
}

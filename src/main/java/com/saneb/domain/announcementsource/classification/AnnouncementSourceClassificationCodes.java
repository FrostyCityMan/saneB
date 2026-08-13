package com.saneb.domain.announcementsource.classification;

/**
 * 공고 분류 V2에서 사용하는 고정 코드 집합입니다.
 */
public final class AnnouncementSourceClassificationCodes {

    private AnnouncementSourceClassificationCodes() {
    }

    public enum SemanticStatusCode {
        ACCEPTED,
        REVIEW_REQUIRED,
        EXCLUDED
    }

    public enum ReasonCode {
        TITLE_GROUP_B_MATCHED,
        TITLE_GROUP_A_MATCHED,
        TITLE_COMBINATION_MATCHED,
        TITLE_COMBINATION_NOT_MATCHED,
        BODY_UNAVAILABLE,
        BODY_FETCH_FAILED,
        BODY_GROUP_B_MATCHED,
        BODY_GROUP_A_MATCHED,
        BODY_COMBINATION_NOT_CONFIRMED,
        TARGET_SUPPORT_CONFIRMED
    }

    public enum RuleGroupKindCode {
        TARGET,
        SUPPORT_TYPE,
        REVIEW_A,
        AUTO_EXCLUDE_B,
        CONTEXT,
        PROTECTED_METADATA
    }

    public enum AppliedActionCode {
        EXCLUDED,
        REVIEW_REQUIRED,
        TAG,
        CONTEXT_ONLY,
        MASK_ONLY
    }

    public enum StrengthCode {
        STRONG,
        SUPPLEMENTARY
    }

    public enum MatchModeCode {
        NORMALIZED_PHRASE,
        TOKEN,
        EXACT_TITLE
    }

    public enum TermTypeCode {
        CANONICAL,
        SYNONYM
    }

    public enum TargetCategoryCode {
        BUSINESS,
        PERSONAL,
        SPOUSE,
        CHILD,
        PARENT
    }

    public enum SupportTypeCode {
        GENERAL_SUPPORT,
        GRANT_SUBSIDY,
        POLICY_FINANCE,
        GUARANTEE,
        INTEREST_SUPPORT,
        VOUCHER_BENEFIT,
        REFUND_REDUCTION
    }

    public enum MatchLocationCode {
        TITLE,
        BODY
    }

    public enum BodySourceCode {
        PROVIDER_FULL_TEXT,
        PROVIDER_SUMMARY,
        DETAIL_PAGE_TEXT,
        NONE
    }

    public enum BodyAvailabilityCode {
        AVAILABLE,
        UNAVAILABLE,
        FETCH_FAILED,
        UNSUPPORTED
    }

    public enum TitleStageCode {
        GROUP_B_MATCHED,
        GROUP_A_MATCHED,
        COMBINATION_MATCHED,
        COMBINATION_NOT_MATCHED
    }

    public enum BodyStageCode {
        NOT_EVALUATED,
        UNAVAILABLE,
        FETCH_FAILED,
        GROUP_B_MATCHED,
        GROUP_A_MATCHED,
        COMBINATION_CONFIRMED,
        COMBINATION_NOT_CONFIRMED
    }
}

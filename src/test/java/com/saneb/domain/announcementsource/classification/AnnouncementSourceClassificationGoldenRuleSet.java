package com.saneb.domain.announcementsource.classification;

import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode.NORMALIZED_PHRASE;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode.TOKEN;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode.AUTO_EXCLUDE_B;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode.PROTECTED_METADATA;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode.REVIEW_A;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode.SUPPORT_TYPE;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode.TARGET;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode.STRONG;
import static com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode.SUPPLEMENTARY;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * QA-01~QA-20에서 사용하는 설계 문서 기반의 최소 DRAFT 규칙 세트입니다.
 */
final class AnnouncementSourceClassificationGoldenRuleSet {

    private AnnouncementSourceClassificationGoldenRuleSet() {
    }

    static AnnouncementSourceClassificationRuleSet selectRuleSet() {
        List<AnnouncementSourceClassificationRule> rules = new ArrayList<>();

        rules.add(rule("TARGET_BUSINESS_SMALL_BUSINESS", "TARGET_BUSINESS", TARGET, "소상공인", STRONG,
                TargetCategoryCode.BUSINESS, null,
                terms(phrase("소상공인"), phrase("자영업자"), phrase("개인사업자"), phrase("사업자"))));
        rules.add(rule("TARGET_BUSINESS_COMPANY", "TARGET_BUSINESS", TARGET, "기업", SUPPLEMENTARY,
                TargetCategoryCode.BUSINESS, null, terms(phrase("기업"))));
        rules.add(rule("TARGET_PERSONAL_YOUTH", "TARGET_PERSONAL", TARGET, "청년", STRONG,
                TargetCategoryCode.PERSONAL, null, terms(phrase("청년"))));
        rules.add(rule("TARGET_PERSONAL_RESIDENT", "TARGET_PERSONAL", TARGET, "주민", SUPPLEMENTARY,
                TargetCategoryCode.PERSONAL, null, terms(phrase("주민"), phrase("지역주민"))));
        rules.add(rule("TARGET_SPOUSE", "TARGET_SPOUSE", TARGET, "배우자", STRONG,
                TargetCategoryCode.SPOUSE, null, terms(phrase("배우자"), phrase("남편"), phrase("아내"))));
        rules.add(rule("TARGET_CHILD", "TARGET_CHILD", TARGET, "자녀", STRONG,
                TargetCategoryCode.CHILD, null, terms(phrase("자녀"), phrase("아동"))));
        rules.add(rule("TARGET_PARENT", "TARGET_PARENT", TARGET, "부모", STRONG,
                TargetCategoryCode.PARENT, null, terms(phrase("부모"), phrase("부모님"))));

        rules.add(rule("SUPPORT_GENERAL_PROGRAM", "SUPPORT_GENERAL_SUPPORT", SUPPORT_TYPE, "지원사업", STRONG,
                null, SupportTypeCode.GENERAL_SUPPORT, terms(phrase("지원사업"))));
        rules.add(rule("SUPPORT_GENERAL_WORD", "SUPPORT_GENERAL_SUPPORT", SUPPORT_TYPE, "지원", SUPPLEMENTARY,
                null, SupportTypeCode.GENERAL_SUPPORT, terms(phrase("지원"))));
        rules.add(rule("SUPPORT_GRANT", "SUPPORT_GRANT_SUBSIDY", SUPPORT_TYPE, "지원금", STRONG,
                null, SupportTypeCode.GRANT_SUBSIDY,
                terms(phrase("지원금"), phrase("보조금"), phrase("장려금"), phrase("보상금"))));
        rules.add(rule("SUPPORT_POLICY_FINANCE", "SUPPORT_POLICY_FINANCE", SUPPORT_TYPE, "정책자금", STRONG,
                null, SupportTypeCode.POLICY_FINANCE,
                terms(phrase("정책자금"), phrase("경영안정자금"), phrase("융자"), phrase("대출"))));
        rules.add(rule("SUPPORT_GUARANTEE", "SUPPORT_GUARANTEE", SUPPORT_TYPE, "보증", STRONG,
                null, SupportTypeCode.GUARANTEE,
                terms(phrase("보증"), phrase("보증료"), phrase("특례보증"), phrase("신용보증"))));
        rules.add(rule("SUPPORT_INTEREST", "SUPPORT_INTEREST_SUPPORT", SUPPORT_TYPE, "이자지원", STRONG,
                null, SupportTypeCode.INTEREST_SUPPORT, terms(phrase("이자지원"), phrase("이차보전"))));
        rules.add(rule("SUPPORT_VOUCHER", "SUPPORT_VOUCHER_BENEFIT", SUPPORT_TYPE, "바우처", STRONG,
                null, SupportTypeCode.VOUCHER_BENEFIT, terms(phrase("바우처"), phrase("쿠폰"), phrase("포인트"))));
        rules.add(rule("SUPPORT_REDUCTION", "SUPPORT_REFUND_REDUCTION", SUPPORT_TYPE, "감면", STRONG,
                null, SupportTypeCode.REFUND_REDUCTION, terms(phrase("감면"), phrase("환급"), phrase("면제"))));

        rules.add(rule("REVIEW_A_SMART_FACTORY", "REVIEW_A_MANUFACTURING_TECH_PRODUCT", REVIEW_A,
                "스마트공장", STRONG, null, null, terms(phrase("스마트공장"))));
        rules.add(rule("REVIEW_A_TECH_STARTUP", "REVIEW_A_MANUFACTURING_TECH_PRODUCT", REVIEW_A,
                "기술창업", STRONG, null, null, terms(phrase("기술창업"))));
        rules.add(rule("REVIEW_A_CERTIFICATION", "REVIEW_A_IP_CERTIFICATION", REVIEW_A,
                "인증", STRONG, null, null, terms(phrase("인증"))));

        rules.add(rule("EXCLUDE_B_EXPORT", "AUTO_EXCLUDE_B_EXPORT", AUTO_EXCLUDE_B,
                "수출", STRONG, null, null, terms(phrase("수출"))));
        rules.add(rule("EXCLUDE_B_STARTUP_TECH", "AUTO_EXCLUDE_B_INVESTMENT_STARTUP", AUTO_EXCLUDE_B,
                "기술창업", STRONG, null, null, terms(phrase("기술창업"))));
        rules.add(rule("EXCLUDE_B_STARTUP_VENTURE", "AUTO_EXCLUDE_B_INVESTMENT_STARTUP", AUTO_EXCLUDE_B,
                "벤처", STRONG, null, null, terms(phrase("벤처"))));
        rules.add(rule("EXCLUDE_B_STARTUP_TIPS", "AUTO_EXCLUDE_B_INVESTMENT_STARTUP", AUTO_EXCLUDE_B,
                "TIPS", STRONG, null, null, terms(token("TIPS"), phrase("팁스"))));
        rules.add(rule("EXCLUDE_B_ADMIN_RECRUIT", "AUTO_EXCLUDE_B_ADMINISTRATIVE", AUTO_EXCLUDE_B,
                "공무원 채용", STRONG, null, null, terms(phrase("공무원 채용"))));
        rules.add(rule("EXCLUDE_B_ADMIN_BID", "AUTO_EXCLUDE_B_ADMINISTRATIVE", AUTO_EXCLUDE_B,
                "공사 입찰", STRONG, null, null, terms(phrase("공사 입찰"))));

        rules.add(rule("PROTECTED_AGENCY_SBC", "PROTECTED_METADATA_AGENCY", PROTECTED_METADATA,
                "중소벤처기업진흥공단", STRONG, null, null,
                terms(phrase("중소벤처기업진흥공단"), token("중진공"))));

        return new AnnouncementSourceClassificationRuleSet("ASCR-QA-V1", rules);
    }

    static AnnouncementSourceClassificationRule selectTokenRule(String termText) {
        return rule("TOKEN_TEST", "AUTO_EXCLUDE_B_TOKEN_TEST", AUTO_EXCLUDE_B, termText, STRONG,
                null, null, terms(token(termText)));
    }

    private static AnnouncementSourceClassificationRule rule(
            String ruleCode,
            String groupCode,
            RuleGroupKindCode groupKindCode,
            String canonicalKeyword,
            StrengthCode strengthCode,
            TargetCategoryCode targetCategoryCode,
            SupportTypeCode supportTypeCode,
            List<AnnouncementSourceClassificationTerm> terms
    ) {
        return new AnnouncementSourceClassificationRule(
                ruleCode,
                groupCode,
                groupKindCode,
                canonicalKeyword,
                strengthCode,
                targetCategoryCode,
                supportTypeCode,
                terms,
                true
        );
    }

    private static List<AnnouncementSourceClassificationTerm> terms(
            TermSeed canonical,
            TermSeed... synonyms
    ) {
        List<AnnouncementSourceClassificationTerm> terms = new ArrayList<>();
        terms.add(new AnnouncementSourceClassificationTerm(
                AnnouncementSourceClassificationCodes.TermTypeCode.CANONICAL,
                canonical.text(),
                canonical.mode(),
                true,
                true
        ));
        Arrays.stream(synonyms).forEach(synonym -> terms.add(new AnnouncementSourceClassificationTerm(
                AnnouncementSourceClassificationCodes.TermTypeCode.SYNONYM,
                synonym.text(),
                synonym.mode(),
                true,
                true
        )));
        return List.copyOf(terms);
    }

    private static TermSeed phrase(String value) {
        return new TermSeed(value, NORMALIZED_PHRASE);
    }

    private static TermSeed token(String value) {
        return new TermSeed(value, TOKEN);
    }

    private record TermSeed(String text, MatchModeCode mode) {
    }
}

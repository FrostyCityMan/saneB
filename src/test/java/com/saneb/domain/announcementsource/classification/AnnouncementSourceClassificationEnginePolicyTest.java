package com.saneb.domain.announcementsource.classification;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TermTypeCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnouncementSourceClassificationEnginePolicyTest {

    private final AnnouncementSourceClassificationEngine engine = new AnnouncementSourceClassificationEngine();

    @Test
    void selectDecisionDoesNotMatchTokenInsideEnglishWord() {
        AnnouncementSourceClassificationRuleSet ruleSet = selectRuleSetWith(
                AnnouncementSourceClassificationGoldenRuleSet.selectTokenRule("IR")
        );

        AnnouncementSourceClassificationResult result = engine.selectDecision(
                input("소상공인 first 지원사업", "소상공인을 위한 지원사업입니다."),
                ruleSet
        );

        assertThat(result.semanticStatusCode().name()).isEqualTo("ACCEPTED");
        assertThat(result.groupBCodes()).doesNotContain("AUTO_EXCLUDE_B_TOKEN_TEST");
    }

    @Test
    void selectDecisionMatchesIndependentTokenCaseInsensitively() {
        AnnouncementSourceClassificationRuleSet ruleSet = selectRuleSetWith(
                AnnouncementSourceClassificationGoldenRuleSet.selectTokenRule("IR")
        );

        AnnouncementSourceClassificationResult result = engine.selectDecision(
                input("소상공인 ir 지원사업", "소상공인을 위한 지원사업입니다."),
                ruleSet
        );

        assertThat(result.semanticStatusCode().name()).isEqualTo("EXCLUDED");
        assertThat(result.reasonCode().name()).isEqualTo("TITLE_GROUP_B_MATCHED");
    }

    @Test
    void selectDecisionSeparatesRuleTermFromOriginalMatchedText() {
        AnnouncementSourceClassificationRuleSet ruleSet = selectRuleSetWith(
                AnnouncementSourceClassificationGoldenRuleSet.selectTokenRule("IR")
        );

        AnnouncementSourceClassificationResult result = engine.selectDecision(
                input("소상공인 ir 지원사업", "소상공인을 위한 지원사업입니다."),
                ruleSet
        );

        assertThat(result.matches())
                .filteredOn(match -> match.ruleCode().equals("TOKEN_TEST"))
                .singleElement()
                .satisfies(match -> {
                    assertThat(match.matchedRuleTerm()).isEqualTo("IR");
                    assertThat(match.matchedTerm()).isEqualTo("ir");
                });
    }

    @Test
    void selectDecisionUsesExactTitleOnlyForWholeNormalizedTitle() {
        AnnouncementSourceClassificationRule exactTitleRule = new AnnouncementSourceClassificationRule(
                "EXACT_TITLE_TEST",
                "AUTO_EXCLUDE_B_EXACT_TEST",
                RuleGroupKindCode.AUTO_EXCLUDE_B,
                "소상공인 지원사업",
                StrengthCode.STRONG,
                null,
                null,
                List.of(new AnnouncementSourceClassificationTerm(
                        TermTypeCode.CANONICAL,
                        "소상공인 지원사업",
                        MatchModeCode.EXACT_TITLE,
                        true,
                        true
                )),
                true
        );
        AnnouncementSourceClassificationRuleSet ruleSet = selectRuleSetWith(exactTitleRule);

        AnnouncementSourceClassificationResult prefixed = engine.selectDecision(
                input("2026년 소상공인 지원사업", "소상공인을 위한 지원사업입니다."),
                ruleSet
        );
        AnnouncementSourceClassificationResult exact = engine.selectDecision(
                input("소상공인 지원사업", "소상공인을 위한 지원사업입니다."),
                ruleSet
        );

        assertThat(prefixed.semanticStatusCode().name()).isEqualTo("ACCEPTED");
        assertThat(exact.semanticStatusCode().name()).isEqualTo("EXCLUDED");
    }

    @Test
    void selectDecisionTreatsRepeatedTitleAsUnavailableBody() {
        AnnouncementSourceClassificationInput input = input(
                "소상공인 정책자금 지원사업",
                "소상공인 정책자금 지원사업"
        );

        AnnouncementSourceClassificationResult result = engine.selectDecision(
                input,
                AnnouncementSourceClassificationGoldenRuleSet.selectRuleSet()
        );

        assertThat(result.semanticStatusCode().name()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.reasonCode().name()).isEqualTo("BODY_UNAVAILABLE");
        assertThat(result.bodyAvailabilityCode().name()).isEqualTo("UNAVAILABLE");
    }

    @Test
    void selectDecisionKeepsBodyFetchFailureSeparateFromMissingBody() {
        AnnouncementSourceClassificationInput input = new AnnouncementSourceClassificationInput(
                "LOCAL_GOV_NOTICE",
                "소상공인 정책자금 지원사업",
                null,
                "테스트시청",
                List.of(),
                BodySourceCode.DETAIL_PAGE_TEXT,
                BodyAvailabilityCode.FETCH_FAILED
        );

        AnnouncementSourceClassificationResult result = engine.selectDecision(
                input,
                AnnouncementSourceClassificationGoldenRuleSet.selectRuleSet()
        );

        assertThat(result.semanticStatusCode().name()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.reasonCode().name()).isEqualTo("BODY_FETCH_FAILED");
        assertThat(result.bodyStageCode().name()).isEqualTo("FETCH_FAILED");
    }

    @Test
    void selectDecisionLoadsProtectedMetadataTermExcludedFromClassification() {
        AnnouncementSourceClassificationRule protectedRule = new AnnouncementSourceClassificationRule(
                "PROTECTED_NON_CLASSIFICATION_TEST",
                "PROTECTED_METADATA_AGENCY",
                RuleGroupKindCode.PROTECTED_METADATA,
                "혁신벤처기관",
                StrengthCode.STRONG,
                null,
                null,
                List.of(new AnnouncementSourceClassificationTerm(
                        TermTypeCode.CANONICAL,
                        "혁신벤처기관",
                        MatchModeCode.NORMALIZED_PHRASE,
                        false,
                        true
                )),
                true
        );
        AnnouncementSourceClassificationRuleSet ruleSet = selectRuleSetWith(protectedRule);

        AnnouncementSourceClassificationResult result = engine.selectDecision(
                input("혁신벤처기관 소상공인 지원사업", "소상공인을 위한 지원사업입니다."),
                ruleSet
        );

        assertThat(result.semanticStatusCode().name()).isEqualTo("ACCEPTED");
        assertThat(result.groupBCodes()).doesNotContain("AUTO_EXCLUDE_B_INVESTMENT_STARTUP");
        assertThat(result.matches())
                .filteredOn(match -> match.ruleCode().equals("PROTECTED_NON_CLASSIFICATION_TEST"))
                .singleElement()
                .satisfies(match -> {
                    assertThat(match.maskedByProtectedMetadata()).isTrue();
                    assertThat(match.appliedActionCode().name()).isEqualTo("MASK_ONLY");
                });
    }

    private AnnouncementSourceClassificationRuleSet selectRuleSetWith(AnnouncementSourceClassificationRule rule) {
        List<AnnouncementSourceClassificationRule> rules = new ArrayList<>(
                AnnouncementSourceClassificationGoldenRuleSet.selectRuleSet().rules()
        );
        rules.add(rule);
        return new AnnouncementSourceClassificationRuleSet("ASCR-POLICY-TEST", rules);
    }

    private AnnouncementSourceClassificationInput input(String title, String bodyText) {
        return new AnnouncementSourceClassificationInput(
                "BIZINFO",
                title,
                bodyText,
                "테스트기관",
                List.of(),
                BodySourceCode.PROVIDER_SUMMARY,
                BodyAvailabilityCode.AVAILABLE
        );
    }
}

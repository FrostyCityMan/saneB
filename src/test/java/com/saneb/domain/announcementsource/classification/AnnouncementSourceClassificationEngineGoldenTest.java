package com.saneb.domain.announcementsource.classification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchLocationCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnnouncementSourceClassificationEngineGoldenTest {

    private static final AnnouncementSourceClassificationRuleSet RULE_SET =
            AnnouncementSourceClassificationGoldenRuleSet.selectRuleSet();
    private static final List<GoldenCase> GOLDEN_CASES = selectGoldenCases();

    private final AnnouncementSourceClassificationEngine engine = new AnnouncementSourceClassificationEngine();

    @ParameterizedTest(name = "{0}")
    @MethodSource("selectGoldenArguments")
    void selectDecisionMatchesQaGoldenSet(String caseId, GoldenCase goldenCase) {
        AnnouncementSourceClassificationResult result = engine.selectDecision(goldenCase.toInput(), RULE_SET);

        assertThat(result.semanticStatusCode().name()).as(caseId + " 상태").isEqualTo(goldenCase.expectedStatusCode());
        assertThat(result.reasonCode().name()).as(caseId + " 사유").isEqualTo(goldenCase.expectedReasonCode());
        assertThat(result.targetCategoryCodes()).extracting(Enum::name)
                .as(caseId + " 지원대상")
                .containsExactlyElementsOf(goldenCase.expectedTargetCategoryCodes());
        assertThat(result.supportTypeCodes()).extracting(Enum::name)
                .as(caseId + " 지원유형")
                .containsExactlyElementsOf(goldenCase.expectedSupportTypeCodes());
        assertThat(result.groupACodes()).as(caseId + " 그룹 A")
                .containsExactlyElementsOf(goldenCase.expectedGroupACodes());
        assertThat(result.groupBCodes()).as(caseId + " 그룹 B")
                .containsExactlyElementsOf(goldenCase.expectedGroupBCodes());
        assertThat(result.matches().stream().map(match -> match.locationCode().name()).collect(java.util.stream.Collectors.toSet()))
                .as(caseId + " 일치 위치")
                .containsExactlyInAnyOrderElementsOf(goldenCase.expectedMatchLocations());
    }

    @Test
    void selectGoldenSetContainsExactlyQa01ThroughQa20() {
        assertThat(GOLDEN_CASES).extracting(GoldenCase::caseId)
                .containsExactly(java.util.stream.IntStream.rangeClosed(1, 20)
                        .mapToObj(number -> "QA-%02d".formatted(number))
                        .toArray(String[]::new));
    }

    @Test
    void selectDecisionMasksAgencyNameButPreservesMaskedEvidence() {
        GoldenCase goldenCase = selectGoldenCase("QA-07");

        AnnouncementSourceClassificationResult result = engine.selectDecision(goldenCase.toInput(), RULE_SET);

        assertThat(result.groupBCodes()).isEmpty();
        assertThat(result.matches()).anySatisfy(match -> {
            assertThat(match.groupCode()).isEqualTo("AUTO_EXCLUDE_B_INVESTMENT_STARTUP");
            assertThat(match.canonicalKeyword()).isEqualTo("벤처");
            assertThat(match.matchedRuleTerm()).isEqualTo("벤처");
            assertThat(match.maskedByProtectedMetadata()).isTrue();
            assertThat(match.appliedActionCode().name()).isEqualTo("MASK_ONLY");
        });
    }

    @Test
    void selectDecisionKeepsTipsAndKoreanSynonymEvidence() {
        AnnouncementSourceClassificationResult result = engine.selectDecision(selectGoldenCase("QA-14").toInput(), RULE_SET);

        assertThat(result.matches().stream()
                .filter(match -> match.groupCode().equals("AUTO_EXCLUDE_B_INVESTMENT_STARTUP"))
                .filter(match -> !match.maskedByProtectedMetadata())
                .map(match -> List.of(match.matchedRuleTerm(), match.matchedTerm())))
                .contains(List.of("TIPS", "TIPS"), List.of("팁스", "팁스"));
    }

    @Test
    void selectDecisionIsProviderNeutralForQa20() {
        GoldenCase goldenCase = selectGoldenCase("QA-20");
        List<AnnouncementSourceClassificationResult> results = Stream.of(
                        "BIZINFO",
                        "GOV24_PUBLIC_SERVICE",
                        "LOCAL_GOV_NOTICE"
                )
                .map(providerCode -> engine.selectDecision(goldenCase.toInput(providerCode), RULE_SET))
                .toList();

        ClassificationProjection expected = ClassificationProjection.from(results.get(0));
        assertThat(results).extracting(ClassificationProjection::from)
                .containsOnly(expected);
    }

    @Test
    void selectClassificationInputHasNoAttachmentContract() {
        Set<String> componentNames = Arrays.stream(AnnouncementSourceClassificationInput.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        assertThat(componentNames).noneMatch(name -> name.contains("attachment") || name.contains("file"));
    }

    private static Stream<Arguments> selectGoldenArguments() {
        return GOLDEN_CASES.stream().map(goldenCase -> Arguments.of(goldenCase.caseId(), goldenCase));
    }

    private static GoldenCase selectGoldenCase(String caseId) {
        return GOLDEN_CASES.stream()
                .filter(goldenCase -> goldenCase.caseId().equals(caseId))
                .findFirst()
                .orElseThrow();
    }

    private static List<GoldenCase> selectGoldenCases() {
        try (InputStream inputStream = AnnouncementSourceClassificationEngineGoldenTest.class
                .getResourceAsStream("/announcement-classification/qa-01-20-golden.json")) {
            if (inputStream == null) {
                throw new IllegalStateException("QA-01~QA-20 golden dataset을 찾을 수 없습니다.");
            }
            return new ObjectMapper().readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("QA-01~QA-20 golden dataset을 읽을 수 없습니다.", exception);
        }
    }

    private record GoldenCase(
            String caseId,
            String providerCode,
            String title,
            String bodyText,
            String agencyName,
            List<String> agencyAliases,
            String bodySourceCode,
            String bodyAvailabilityCode,
            String expectedStatusCode,
            String expectedReasonCode,
            List<String> expectedTargetCategoryCodes,
            List<String> expectedSupportTypeCodes,
            List<String> expectedGroupACodes,
            List<String> expectedGroupBCodes,
            List<String> expectedMatchLocations,
            String note
    ) {

        AnnouncementSourceClassificationInput toInput() {
            return toInput(providerCode);
        }

        AnnouncementSourceClassificationInput toInput(String overriddenProviderCode) {
            return new AnnouncementSourceClassificationInput(
                    overriddenProviderCode,
                    title,
                    bodyText,
                    agencyName,
                    agencyAliases,
                    BodySourceCode.valueOf(bodySourceCode),
                    BodyAvailabilityCode.valueOf(bodyAvailabilityCode)
            );
        }
    }

    private record ClassificationProjection(
            String statusCode,
            String reasonCode,
            String titleStageCode,
            String bodyStageCode,
            List<String> targetCategoryCodes,
            List<String> supportTypeCodes,
            List<String> groupACodes,
            List<String> groupBCodes,
            List<String> matchEvidence
    ) {

        static ClassificationProjection from(AnnouncementSourceClassificationResult result) {
            return new ClassificationProjection(
                    result.semanticStatusCode().name(),
                    result.reasonCode().name(),
                    result.titleStageCode().name(),
                    result.bodyStageCode().name(),
                    result.targetCategoryCodes().stream().map(Enum::name).toList(),
                    result.supportTypeCodes().stream().map(Enum::name).toList(),
                    result.groupACodes(),
                    result.groupBCodes(),
                    result.matches().stream()
                            .map(match -> match.ruleCode() + ":" + match.locationCode() + ":"
                                    + match.startOffset() + ":" + match.endOffset() + ":"
                                    + match.appliedActionCode())
                            .toList()
            );
        }
    }
}

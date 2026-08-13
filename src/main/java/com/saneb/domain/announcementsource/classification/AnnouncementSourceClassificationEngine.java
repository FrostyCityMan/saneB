package com.saneb.domain.announcementsource.classification;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.AppliedActionCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyStageCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchLocationCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.ReasonCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SemanticStatusCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 제목·본문과 고정된 규칙 release만으로 판정하는 순수 Java 분류 엔진입니다.
 */
public final class AnnouncementSourceClassificationEngine {

    private static final Comparator<AnnouncementSourceClassificationMatch> MATCH_ORDER = Comparator
            .comparing(AnnouncementSourceClassificationMatch::locationCode)
            .thenComparingInt(AnnouncementSourceClassificationMatch::startOffset)
            .thenComparingInt(AnnouncementSourceClassificationMatch::endOffset)
            .thenComparing(AnnouncementSourceClassificationMatch::groupCode)
            .thenComparing(AnnouncementSourceClassificationMatch::ruleCode)
            .thenComparing(AnnouncementSourceClassificationMatch::matchedRuleTerm)
            .thenComparing(AnnouncementSourceClassificationMatch::matchedTerm);

    private final AnnouncementSourceTextNormalizer normalizer;

    public AnnouncementSourceClassificationEngine() {
        this(new AnnouncementSourceTextNormalizer());
    }

    public AnnouncementSourceClassificationEngine(AnnouncementSourceTextNormalizer normalizer) {
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer is required");
    }

    public AnnouncementSourceClassificationResult selectDecision(
            AnnouncementSourceClassificationInput input,
            AnnouncementSourceClassificationRuleSet ruleSet
    ) {
        Objects.requireNonNull(input, "input is required");
        Objects.requireNonNull(ruleSet, "ruleSet is required");

        List<CompiledRuleTerm> compiledTerms = selectCompiledTerms(ruleSet);
        AnnouncementSourceNormalizedText normalizedTitle = normalizer.selectNormalizedText(input.title());
        LocationEvaluation titleEvaluation = selectLocationEvaluation(
                input,
                normalizedTitle,
                MatchLocationCode.TITLE,
                compiledTerms
        );

        if (!titleEvaluation.groupBCodes().isEmpty()) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.EXCLUDED,
                    ReasonCode.TITLE_GROUP_B_MATCHED,
                    TitleStageCode.GROUP_B_MATCHED,
                    BodyStageCode.NOT_EVALUATED,
                    input.bodyAvailabilityCode(),
                    titleEvaluation,
                    null
            );
        }

        boolean titleGroupAMatched = !titleEvaluation.groupACodes().isEmpty();
        boolean titleCombinationMatched = titleEvaluation.combinationMatched();
        TitleStageCode titleStageCode = titleGroupAMatched
                ? TitleStageCode.GROUP_A_MATCHED
                : titleCombinationMatched
                        ? TitleStageCode.COMBINATION_MATCHED
                        : TitleStageCode.COMBINATION_NOT_MATCHED;

        if (!titleGroupAMatched && !titleCombinationMatched) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.EXCLUDED,
                    ReasonCode.TITLE_COMBINATION_NOT_MATCHED,
                    titleStageCode,
                    BodyStageCode.NOT_EVALUATED,
                    input.bodyAvailabilityCode(),
                    titleEvaluation,
                    null
            );
        }

        AnnouncementSourceNormalizedText normalizedBody = normalizer.selectNormalizedText(input.bodyText());
        BodyAvailabilityCode bodyAvailabilityCode = selectEffectiveBodyAvailability(
                input,
                normalizedTitle,
                normalizedBody
        );
        if (bodyAvailabilityCode == BodyAvailabilityCode.FETCH_FAILED) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.REVIEW_REQUIRED,
                    ReasonCode.BODY_FETCH_FAILED,
                    titleStageCode,
                    BodyStageCode.FETCH_FAILED,
                    bodyAvailabilityCode,
                    titleEvaluation,
                    null
            );
        }
        if (bodyAvailabilityCode != BodyAvailabilityCode.AVAILABLE) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.REVIEW_REQUIRED,
                    ReasonCode.BODY_UNAVAILABLE,
                    titleStageCode,
                    BodyStageCode.UNAVAILABLE,
                    bodyAvailabilityCode,
                    titleEvaluation,
                    null
            );
        }

        LocationEvaluation bodyEvaluation = selectLocationEvaluation(
                input,
                normalizedBody,
                MatchLocationCode.BODY,
                compiledTerms
        );
        if (!bodyEvaluation.groupBCodes().isEmpty()) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.REVIEW_REQUIRED,
                    ReasonCode.BODY_GROUP_B_MATCHED,
                    titleStageCode,
                    BodyStageCode.GROUP_B_MATCHED,
                    bodyAvailabilityCode,
                    titleEvaluation,
                    bodyEvaluation
            );
        }
        if (!bodyEvaluation.groupACodes().isEmpty()) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.REVIEW_REQUIRED,
                    ReasonCode.BODY_GROUP_A_MATCHED,
                    titleStageCode,
                    BodyStageCode.GROUP_A_MATCHED,
                    bodyAvailabilityCode,
                    titleEvaluation,
                    bodyEvaluation
            );
        }
        if (!bodyEvaluation.combinationMatched()) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.REVIEW_REQUIRED,
                    ReasonCode.BODY_COMBINATION_NOT_CONFIRMED,
                    titleStageCode,
                    BodyStageCode.COMBINATION_NOT_CONFIRMED,
                    bodyAvailabilityCode,
                    titleEvaluation,
                    bodyEvaluation
            );
        }
        if (titleGroupAMatched) {
            return selectResult(
                    input,
                    ruleSet,
                    SemanticStatusCode.REVIEW_REQUIRED,
                    ReasonCode.TITLE_GROUP_A_MATCHED,
                    titleStageCode,
                    BodyStageCode.COMBINATION_CONFIRMED,
                    bodyAvailabilityCode,
                    titleEvaluation,
                    bodyEvaluation
            );
        }
        return selectResult(
                input,
                ruleSet,
                SemanticStatusCode.ACCEPTED,
                ReasonCode.TARGET_SUPPORT_CONFIRMED,
                titleStageCode,
                BodyStageCode.COMBINATION_CONFIRMED,
                bodyAvailabilityCode,
                titleEvaluation,
                bodyEvaluation
        );
    }

    private List<CompiledRuleTerm> selectCompiledTerms(AnnouncementSourceClassificationRuleSet ruleSet) {
        List<CompiledRuleTerm> compiled = new ArrayList<>();
        for (AnnouncementSourceClassificationRule rule : ruleSet.rules()) {
            if (!rule.enabled()) {
                continue;
            }
            for (AnnouncementSourceClassificationTerm term : rule.terms()) {
                boolean protectionTerm = rule.groupKindCode() == RuleGroupKindCode.PROTECTED_METADATA;
                if (!term.enabled() || (!term.classificationTerm() && !protectionTerm)) {
                    continue;
                }
                AnnouncementSourceNormalizedText normalizedTerm = normalizer.selectNormalizedText(term.termText());
                if (normalizedTerm.length() > 0) {
                    compiled.add(new CompiledRuleTerm(rule, term, normalizedTerm.normalizedCodePoints()));
                }
            }
        }
        return List.copyOf(compiled);
    }

    private LocationEvaluation selectLocationEvaluation(
            AnnouncementSourceClassificationInput input,
            AnnouncementSourceNormalizedText text,
            MatchLocationCode locationCode,
            List<CompiledRuleTerm> compiledTerms
    ) {
        List<NormalizedSpan> protectedSpans = selectProtectedSpans(input, text, locationCode, compiledTerms);
        List<AnnouncementSourceClassificationMatch> matches = new ArrayList<>();
        Map<TargetCategoryCode, StrengthCode> targetStrengths = new EnumMap<>(TargetCategoryCode.class);
        Map<SupportTypeCode, StrengthCode> supportStrengths = new EnumMap<>(SupportTypeCode.class);
        Set<String> groupACodes = new LinkedHashSet<>();
        Set<String> groupBCodes = new LinkedHashSet<>();

        for (CompiledRuleTerm compiledTerm : compiledTerms) {
            if (compiledTerm.term().matchModeCode() == MatchModeCode.EXACT_TITLE
                    && locationCode != MatchLocationCode.TITLE) {
                continue;
            }
            for (NormalizedSpan occurrence : selectOccurrenceList(text, compiledTerm)) {
                boolean protectedMetadataRule = compiledTerm.rule().groupKindCode()
                        == RuleGroupKindCode.PROTECTED_METADATA;
                boolean masked = protectedMetadataRule || selectContainedByProtectedSpan(occurrence, protectedSpans);
                AnnouncementSourceNormalizedText.OriginalRange originalRange = text.selectOriginalRange(
                        occurrence.startOffset(),
                        occurrence.endOffset()
                );
                AppliedActionCode actionCode = masked
                        ? AppliedActionCode.MASK_ONLY
                        : selectAppliedAction(compiledTerm.rule().groupKindCode(), locationCode);
                matches.add(new AnnouncementSourceClassificationMatch(
                        compiledTerm.rule().ruleCode(),
                        compiledTerm.rule().groupCode(),
                        compiledTerm.rule().groupKindCode(),
                        compiledTerm.rule().canonicalKeyword(),
                        compiledTerm.term().termText(),
                        selectOriginalSubstring(text.originalText(), originalRange),
                        locationCode,
                        originalRange.startOffset(),
                        originalRange.endOffset(),
                        actionCode,
                        masked
                ));
                if (!masked) {
                    applyClassificationMatch(
                            compiledTerm.rule(),
                            targetStrengths,
                            supportStrengths,
                            groupACodes,
                            groupBCodes
                    );
                }
            }
        }
        matches.sort(MATCH_ORDER);
        return new LocationEvaluation(
                List.copyOf(matches),
                Map.copyOf(targetStrengths),
                Map.copyOf(supportStrengths),
                List.copyOf(groupACodes),
                List.copyOf(groupBCodes),
                selectCombinationMatched(targetStrengths, supportStrengths)
        );
    }

    private List<NormalizedSpan> selectProtectedSpans(
            AnnouncementSourceClassificationInput input,
            AnnouncementSourceNormalizedText text,
            MatchLocationCode locationCode,
            List<CompiledRuleTerm> compiledTerms
    ) {
        List<NormalizedSpan> spans = new ArrayList<>();
        List<String> mappedAgencyNames = new ArrayList<>();
        if (input.agencyName() != null && !input.agencyName().isBlank()) {
            mappedAgencyNames.add(input.agencyName());
        }
        mappedAgencyNames.addAll(input.agencyAliases());
        Set<String> normalizedMappedAgencyNames = new LinkedHashSet<>();
        for (String agencyName : mappedAgencyNames) {
            AnnouncementSourceNormalizedText normalizedAgency = normalizer.selectNormalizedText(agencyName);
            if (normalizedAgency.length() == 0) {
                continue;
            }
            normalizedMappedAgencyNames.add(normalizedAgency.normalizedText());
            MatchModeCode matchModeCode = normalizedAgency.length() <= 5
                    ? MatchModeCode.TOKEN
                    : MatchModeCode.NORMALIZED_PHRASE;
            spans.addAll(selectOccurrenceList(text, normalizedAgency.normalizedCodePoints(), matchModeCode));
        }

        for (CompiledRuleTerm compiledTerm : compiledTerms) {
            if (compiledTerm.rule().groupKindCode() != RuleGroupKindCode.PROTECTED_METADATA) {
                continue;
            }
            boolean mapped = normalizedMappedAgencyNames.contains(
                    new String(compiledTerm.normalizedCodePoints(), 0, compiledTerm.normalizedCodePoints().length)
            );
            boolean longOfficialName = compiledTerm.normalizedCodePoints().length >= 4
                    && compiledTerm.term().matchModeCode() == MatchModeCode.NORMALIZED_PHRASE;
            List<NormalizedSpan> occurrences = selectOccurrenceList(text, compiledTerm);
            for (NormalizedSpan occurrence : occurrences) {
                boolean titleLeadingAlias = locationCode == MatchLocationCode.TITLE && occurrence.startOffset() == 0;
                if (mapped || longOfficialName || titleLeadingAlias) {
                    spans.add(occurrence);
                }
            }
        }
        return List.copyOf(spans);
    }

    private List<NormalizedSpan> selectOccurrenceList(
            AnnouncementSourceNormalizedText source,
            CompiledRuleTerm compiledTerm
    ) {
        return selectOccurrenceList(
                source,
                compiledTerm.normalizedCodePoints(),
                compiledTerm.term().matchModeCode()
        );
    }

    private List<NormalizedSpan> selectOccurrenceList(
            AnnouncementSourceNormalizedText source,
            int[] termCodePoints,
            MatchModeCode matchModeCode
    ) {
        int[] sourceCodePoints = source.normalizedCodePoints();
        if (termCodePoints.length == 0 || sourceCodePoints.length < termCodePoints.length) {
            return List.of();
        }
        if (matchModeCode == MatchModeCode.EXACT_TITLE) {
            return java.util.Arrays.equals(sourceCodePoints, termCodePoints)
                    ? List.of(new NormalizedSpan(0, sourceCodePoints.length))
                    : List.of();
        }
        List<NormalizedSpan> occurrences = new ArrayList<>();
        for (int start = 0; start <= sourceCodePoints.length - termCodePoints.length; start++) {
            if (!selectMatchesAt(sourceCodePoints, termCodePoints, start)) {
                continue;
            }
            int end = start + termCodePoints.length;
            if (matchModeCode == MatchModeCode.TOKEN
                    && (!selectTokenBoundary(sourceCodePoints, start - 1)
                    || !selectTokenBoundary(sourceCodePoints, end))) {
                continue;
            }
            occurrences.add(new NormalizedSpan(start, end));
        }
        return List.copyOf(occurrences);
    }

    private boolean selectMatchesAt(int[] sourceCodePoints, int[] termCodePoints, int start) {
        for (int index = 0; index < termCodePoints.length; index++) {
            if (sourceCodePoints[start + index] != termCodePoints[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean selectTokenBoundary(int[] sourceCodePoints, int index) {
        return index < 0
                || index >= sourceCodePoints.length
                || !Character.isLetterOrDigit(sourceCodePoints[index]);
    }

    private boolean selectContainedByProtectedSpan(NormalizedSpan occurrence, List<NormalizedSpan> protectedSpans) {
        return protectedSpans.stream().anyMatch(protectedSpan -> occurrence.startOffset() >= protectedSpan.startOffset()
                && occurrence.endOffset() <= protectedSpan.endOffset());
    }

    private AppliedActionCode selectAppliedAction(
            RuleGroupKindCode groupKindCode,
            MatchLocationCode locationCode
    ) {
        return switch (groupKindCode) {
            case TARGET, SUPPORT_TYPE -> AppliedActionCode.TAG;
            case REVIEW_A -> AppliedActionCode.REVIEW_REQUIRED;
            case AUTO_EXCLUDE_B -> locationCode == MatchLocationCode.TITLE
                    ? AppliedActionCode.EXCLUDED
                    : AppliedActionCode.REVIEW_REQUIRED;
            case CONTEXT -> AppliedActionCode.CONTEXT_ONLY;
            case PROTECTED_METADATA -> AppliedActionCode.MASK_ONLY;
        };
    }

    private void applyClassificationMatch(
            AnnouncementSourceClassificationRule rule,
            Map<TargetCategoryCode, StrengthCode> targetStrengths,
            Map<SupportTypeCode, StrengthCode> supportStrengths,
            Set<String> groupACodes,
            Set<String> groupBCodes
    ) {
        switch (rule.groupKindCode()) {
            case TARGET -> targetStrengths.merge(
                    rule.targetCategoryCode(),
                    rule.strengthCode(),
                    this::selectStronger
            );
            case SUPPORT_TYPE -> supportStrengths.merge(
                    rule.supportTypeCode(),
                    rule.strengthCode(),
                    this::selectStronger
            );
            case REVIEW_A -> groupACodes.add(rule.groupCode());
            case AUTO_EXCLUDE_B -> groupBCodes.add(rule.groupCode());
            case CONTEXT, PROTECTED_METADATA -> {
                // 분류 상태와 태그에 영향을 주지 않습니다.
            }
        }
    }

    private StrengthCode selectStronger(StrengthCode left, StrengthCode right) {
        return left == StrengthCode.STRONG || right == StrengthCode.STRONG
                ? StrengthCode.STRONG
                : StrengthCode.SUPPLEMENTARY;
    }

    private boolean selectCombinationMatched(
            Map<TargetCategoryCode, StrengthCode> targetStrengths,
            Map<SupportTypeCode, StrengthCode> supportStrengths
    ) {
        if (targetStrengths.isEmpty() || supportStrengths.isEmpty()) {
            return false;
        }
        return targetStrengths.containsValue(StrengthCode.STRONG)
                || supportStrengths.containsValue(StrengthCode.STRONG);
    }

    private BodyAvailabilityCode selectEffectiveBodyAvailability(
            AnnouncementSourceClassificationInput input,
            AnnouncementSourceNormalizedText normalizedTitle,
            AnnouncementSourceNormalizedText normalizedBody
    ) {
        if (input.bodyAvailabilityCode() == BodyAvailabilityCode.FETCH_FAILED) {
            return BodyAvailabilityCode.FETCH_FAILED;
        }
        if (input.bodyAvailabilityCode() != BodyAvailabilityCode.AVAILABLE
                || normalizedBody.length() == 0
                || normalizedBody.normalizedText().equals(normalizedTitle.normalizedText())) {
            return input.bodyAvailabilityCode() == BodyAvailabilityCode.UNSUPPORTED
                    ? BodyAvailabilityCode.UNSUPPORTED
                    : BodyAvailabilityCode.UNAVAILABLE;
        }
        return BodyAvailabilityCode.AVAILABLE;
    }

    private AnnouncementSourceClassificationResult selectResult(
            AnnouncementSourceClassificationInput input,
            AnnouncementSourceClassificationRuleSet ruleSet,
            SemanticStatusCode semanticStatusCode,
            ReasonCode reasonCode,
            TitleStageCode titleStageCode,
            BodyStageCode bodyStageCode,
            BodyAvailabilityCode bodyAvailabilityCode,
            LocationEvaluation titleEvaluation,
            LocationEvaluation bodyEvaluation
    ) {
        List<LocationEvaluation> evaluations = bodyEvaluation == null
                ? List.of(titleEvaluation)
                : List.of(titleEvaluation, bodyEvaluation);
        Set<TargetCategoryCode> targetCategoryCodes = new LinkedHashSet<>();
        Set<SupportTypeCode> supportTypeCodes = new LinkedHashSet<>();
        Set<String> groupACodes = new LinkedHashSet<>();
        Set<String> groupBCodes = new LinkedHashSet<>();
        List<AnnouncementSourceClassificationMatch> matches = new ArrayList<>();
        for (LocationEvaluation evaluation : evaluations) {
            targetCategoryCodes.addAll(evaluation.targetStrengths().keySet());
            supportTypeCodes.addAll(evaluation.supportStrengths().keySet());
            groupACodes.addAll(evaluation.groupACodes());
            groupBCodes.addAll(evaluation.groupBCodes());
            matches.addAll(evaluation.matches());
        }
        matches.sort(MATCH_ORDER);
        return new AnnouncementSourceClassificationResult(
                input.providerCode(),
                ruleSet.releaseCode(),
                semanticStatusCode,
                reasonCode,
                titleStageCode,
                bodyStageCode,
                input.bodySourceCode(),
                bodyAvailabilityCode,
                targetCategoryCodes.stream().sorted().toList(),
                supportTypeCodes.stream().sorted().toList(),
                groupACodes.stream().sorted().toList(),
                groupBCodes.stream().sorted().toList(),
                matches
        );
    }

    private String selectOriginalSubstring(
            String source,
            AnnouncementSourceNormalizedText.OriginalRange range
    ) {
        int startIndex = source.offsetByCodePoints(0, range.startOffset());
        int endIndex = source.offsetByCodePoints(0, range.endOffset());
        return source.substring(startIndex, endIndex);
    }

    private record CompiledRuleTerm(
            AnnouncementSourceClassificationRule rule,
            AnnouncementSourceClassificationTerm term,
            int[] normalizedCodePoints
    ) {
    }

    private record NormalizedSpan(int startOffset, int endOffset) {
    }

    private record LocationEvaluation(
            List<AnnouncementSourceClassificationMatch> matches,
            Map<TargetCategoryCode, StrengthCode> targetStrengths,
            Map<SupportTypeCode, StrengthCode> supportStrengths,
            List<String> groupACodes,
            List<String> groupBCodes,
            boolean combinationMatched
    ) {
    }
}

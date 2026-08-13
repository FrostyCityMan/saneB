package com.saneb.domain.announcementsource.service.impl;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TitleStageCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationEngine;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationInput;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceContentHasher;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlanBuilder;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceActiveRuleService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationCoordinator;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementSourceClassificationCoordinatorImpl
        implements AnnouncementSourceClassificationCoordinator {

    private final boolean enabled;
    private final AnnouncementSourceActiveRuleService activeRuleService;
    private final AnnouncementSourceClassificationDao classificationDao;
    private final AnnouncementSourceSearchPlanBuilder searchPlanBuilder;
    private final AnnouncementSourceClassificationPersistenceService persistenceService;
    private final AnnouncementSourceClassificationEngine engine;
    private final int maximumSearchCombinationCount;

    public AnnouncementSourceClassificationCoordinatorImpl(
            @Value("${saneb.announcement-source.classification-v2.enabled:false}") boolean enabled,
            AnnouncementSourceActiveRuleService activeRuleService,
            AnnouncementSourceClassificationDao classificationDao,
            AnnouncementSourceSearchPlanBuilder searchPlanBuilder,
            AnnouncementSourceClassificationPersistenceService persistenceService,
            @Value("${saneb.announcement-source.classification-v2.maximum-search-combinations:50}")
            int maximumSearchCombinationCount
    ) {
        this.enabled = enabled;
        this.activeRuleService = activeRuleService;
        this.classificationDao = classificationDao;
        this.searchPlanBuilder = searchPlanBuilder;
        this.persistenceService = persistenceService;
        this.engine = new AnnouncementSourceClassificationEngine();
        this.maximumSearchCombinationCount = maximumSearchCombinationCount;
    }

    @Override
    public RunContext selectRunContext(UUID runId, String providerCode) {
        if (!enabled) {
            return RunContext.disabled();
        }
        AnnouncementSourceActiveRuleService.ActiveRuleSet activeRuleSet = activeRuleService.selectActiveRuleSet();
        AnnouncementSourceSearchPlan searchPlan = searchPlanBuilder.selectPlan(
                activeRuleSet.releaseId(),
                providerCode,
                activeRuleSet.discoveryTerms(),
                maximumSearchCombinationCount
        );
        if (classificationDao.updateCollectionRunRuleRelease(
                runId,
                activeRuleSet.releaseId(),
                searchPlan.sha256(),
                searchPlan.canonicalJson()
        ) == 0) {
            throw new IllegalStateException("수집 실행에 공고 분류 규칙 버전과 검색 계획을 고정하지 못했습니다.");
        }
        return new RunContext(true, activeRuleSet.releaseId(), activeRuleSet.ruleSet(), searchPlan);
    }

    @Override
    public PreparedClassification selectClassification(
            RunContext runContext,
            AnnouncementSourceProviderItem item,
            BodySourceCode bodySourceCode,
            BodyAvailabilityCode bodyAvailabilityCode
    ) {
        if (!runContext.enabled()) {
            return PreparedClassification.disabled(item);
        }
        AnnouncementSourceClassificationResult result = engine.selectDecision(
                new AnnouncementSourceClassificationInput(
                        item.providerCode(),
                        item.title(),
                        item.bodyText(),
                        item.agencyName(),
                        List.of(),
                        bodySourceCode,
                        bodyAvailabilityCode
                ),
                runContext.ruleSet()
        );
        String matchedKeywords = result.matches().stream()
                .filter(match -> !match.maskedByProtectedMetadata())
                .map(match -> match.matchedTerm())
                .distinct()
                .collect(Collectors.joining(", "));
        if (matchedKeywords.length() > 1000) {
            matchedKeywords = matchedKeywords.substring(0, 1000);
        }
        AnnouncementSourceProviderItem classifiedItem = item.withSemanticDecision(
                result.semanticStatusCode().name(),
                result.reasonCode().name(),
                matchedKeywords.isBlank() ? null : matchedKeywords
        );
        return new PreparedClassification(true, runContext.ruleReleaseId(), classifiedItem, result);
    }

    @Override
    public boolean selectBodyFetchRequired(
            RunContext runContext,
            AnnouncementSourceProviderItem item
    ) {
        if (!runContext.enabled()) {
            return false;
        }
        AnnouncementSourceClassificationResult titleDecision = engine.selectDecision(
                new AnnouncementSourceClassificationInput(
                        item.providerCode(),
                        item.title(),
                        null,
                        item.agencyName(),
                        List.of(),
                        BodySourceCode.NONE,
                        BodyAvailabilityCode.UNAVAILABLE
                ),
                runContext.ruleSet()
        );
        return titleDecision.titleStageCode() == TitleStageCode.GROUP_A_MATCHED
                || titleDecision.titleStageCode() == TitleStageCode.COMBINATION_MATCHED;
    }

    @Override
    public boolean selectContentVersionAppendRequired(
            UUID sourceId,
            PreparedClassification preparedClassification
    ) {
        if (!preparedClassification.enabled()) {
            return false;
        }
        String nextContentHash = AnnouncementSourceContentHasher.selectHash(
                preparedClassification.item(),
                preparedClassification.result()
        );
        String currentContentHash = classificationDao.selectLatestContentVersionHash(sourceId);
        return !nextContentHash.equals(currentContentHash);
    }

    @Override
    @Transactional
    public void saveClassification(
            UUID sourceId,
            UUID runId,
            PreparedClassification preparedClassification,
            String reviewStatusCode
    ) {
        if (!preparedClassification.enabled()) {
            return;
        }
        persistenceService.saveNewContentEvaluation(
                sourceId,
                runId,
                preparedClassification.ruleReleaseId(),
                preparedClassification.item(),
                preparedClassification.result(),
                reviewStatusCode
        );
    }

    @Override
    @Transactional
    public void saveChangedClassification(
            UUID sourceId,
            UUID runId,
            PreparedClassification preparedClassification,
            String reviewStatusCode,
            int expectedVersion
    ) {
        if (!preparedClassification.enabled()) {
            return;
        }
        persistenceService.saveChangedContentEvaluation(
                sourceId,
                runId,
                preparedClassification.ruleReleaseId(),
                preparedClassification.item(),
                preparedClassification.result(),
                reviewStatusCode,
                expectedVersion
        );
    }
}

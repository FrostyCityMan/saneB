package com.saneb.domain.announcementsource.service.impl;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceContentHasher;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationPersistenceService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationEvaluationCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationMatchCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationSupportMatchCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationTargetMatchCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceContentVersionCommand;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementSourceClassificationPersistenceServiceImpl
        implements AnnouncementSourceClassificationPersistenceService {

    private static final String ENGINE_VERSION = "RULE_V2_1";

    private final AnnouncementSourceClassificationDao classificationDao;

    public AnnouncementSourceClassificationPersistenceServiceImpl(
            AnnouncementSourceClassificationDao classificationDao
    ) {
        this.classificationDao = classificationDao;
    }

    @Override
    @Transactional
    public UUID saveNewContentEvaluation(
            UUID sourceId,
            UUID runId,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result,
            String reviewStatusCode
    ) {
        UUID contentVersionId = insertContentVersion(sourceId, item, result);
        return saveEvaluation(
                sourceId, contentVersionId, runId, ruleReleaseId, item, result, reviewStatusCode, null
        );
    }

    @Override
    @Transactional
    public UUID saveExistingContentEvaluation(
            UUID sourceId,
            UUID contentVersionId,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result,
            String reviewStatusCode,
            int expectedVersion
    ) {
        return saveEvaluation(
                sourceId, contentVersionId, null, ruleReleaseId, item, result, reviewStatusCode, expectedVersion
        );
    }

    @Override
    @Transactional
    public UUID saveChangedContentEvaluation(
            UUID sourceId,
            UUID runId,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result,
            String reviewStatusCode,
            int expectedVersion
    ) {
        UUID contentVersionId = insertContentVersion(sourceId, item, result);
        return saveEvaluation(
                sourceId,
                contentVersionId,
                runId,
                ruleReleaseId,
                item,
                result,
                reviewStatusCode,
                expectedVersion
        );
    }

    private UUID insertContentVersion(
            UUID sourceId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result
    ) {
        String contentHash = AnnouncementSourceContentHasher.selectHash(item, result);
        classificationDao.insertContentVersion(new AnnouncementSourceContentVersionCommand(
                UUID.randomUUID(),
                sourceId,
                contentHash,
                item.title(),
                item.bodyText(),
                result.bodySourceCode().name(),
                result.bodyAvailabilityCode().name(),
                item.sourceUrl(),
                item.rawPayloadJson(),
                OffsetDateTime.now()
        ));
        UUID contentVersionId = classificationDao.selectContentVersionId(sourceId, contentHash);
        if (contentVersionId == null) {
            throw new IllegalStateException("공고 원문 버전을 저장하지 못했습니다.");
        }
        return contentVersionId;
    }

    private UUID saveEvaluation(
            UUID sourceId,
            UUID contentVersionId,
            UUID runId,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result,
            String reviewStatusCode,
            Integer expectedVersion
    ) {
        classificationDao.updateCurrentEvaluationNotCurrent(sourceId);
        classificationDao.updateConfirmedTargetClassificationStale(sourceId);
        classificationDao.updateConfirmedSupportClassificationStale(sourceId);

        UUID evaluationId = UUID.randomUUID();
        classificationDao.insertClassificationEvaluation(new AnnouncementSourceClassificationEvaluationCommand(
                evaluationId,
                sourceId,
                contentVersionId,
                runId,
                ruleReleaseId,
                ENGINE_VERSION,
                result.bodySourceCode().name(),
                result.bodyAvailabilityCode().name(),
                result.titleStageCode().name(),
                result.bodyStageCode().name(),
                result.semanticStatusCode().name(),
                result.reasonCode().name()
        ));
        result.matches().forEach(match -> requireInserted(
                classificationDao.insertClassificationMatch(new AnnouncementSourceClassificationMatchCommand(
                        UUID.randomUUID(),
                        evaluationId,
                        ruleReleaseId,
                        match.ruleCode(),
                        match.matchedRuleTerm(),
                        match.matchedTerm(),
                        match.locationCode().name(),
                        match.startOffset(),
                        match.endOffset(),
                        match.appliedActionCode().name()
                )),
                "판정 일치 근거"
        ));
        result.targetCategoryCodes().forEach(code -> requireInserted(
                classificationDao.insertClassificationTargetMatch(
                        new AnnouncementSourceClassificationTargetMatchCommand(
                                UUID.randomUUID(), evaluationId, code.name()
                        )
                ),
                "지원대상 자동 태그"
        ));
        result.supportTypeCodes().forEach(code -> requireInserted(
                classificationDao.insertClassificationSupportMatch(
                        new AnnouncementSourceClassificationSupportMatchCommand(
                                UUID.randomUUID(), evaluationId, code.name()
                        )
                ),
                "지원형태 자동 태그"
        ));

        String matchedKeywords = new LinkedHashSet<>(result.matches().stream()
                .filter(match -> !match.maskedByProtectedMetadata())
                .map(match -> match.matchedTerm())
                .toList()).stream().collect(Collectors.joining(", "));
        if (matchedKeywords.length() > 1000) {
            matchedKeywords = matchedKeywords.substring(0, 1000);
        }
        int updatedCount = classificationDao.updateSnapshotClassificationProjection(
                sourceId,
                result.semanticStatusCode().name(),
                result.reasonCode().name(),
                matchedKeywords.isBlank() ? null : matchedKeywords,
                reviewStatusCode,
                expectedVersion
        );
        if (updatedCount != 1) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_VERSION_CONFLICT,
                    HttpStatus.CONFLICT,
                    "다른 작업자가 판정을 변경했습니다. 최신 상태를 확인한 뒤 다시 시도해 주세요."
            );
        }
        return evaluationId;
    }

    private void requireInserted(int insertedCount, String evidenceLabel) {
        if (insertedCount != 1) {
            throw new IllegalStateException(evidenceLabel + "를 정확히 한 건 저장하지 못했습니다.");
        }
    }

}

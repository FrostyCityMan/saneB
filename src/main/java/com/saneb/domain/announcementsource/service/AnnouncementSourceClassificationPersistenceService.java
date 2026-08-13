package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import java.util.UUID;

/**
 * 분류 원문 버전과 판정 근거를 짧은 DB transaction으로 보존합니다.
 */
public interface AnnouncementSourceClassificationPersistenceService {

    UUID saveNewContentEvaluation(
            UUID sourceId,
            UUID runId,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result,
            String reviewStatusCode
    );

    UUID saveExistingContentEvaluation(
            UUID sourceId,
            UUID contentVersionId,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result,
            String reviewStatusCode,
            int expectedVersion
    );

    UUID saveChangedContentEvaluation(
            UUID sourceId,
            UUID runId,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result,
            String reviewStatusCode,
            int expectedVersion
    );
}

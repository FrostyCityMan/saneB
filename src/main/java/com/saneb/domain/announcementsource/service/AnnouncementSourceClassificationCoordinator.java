/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceClassificationCoordinator.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationResult;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceSearchPlan;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodyAvailabilityCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.BodySourceCode;
import com.saneb.domain.announcementsource.provider.AnnouncementSourceProviderItem;
import java.util.UUID;

public interface AnnouncementSourceClassificationCoordinator {

    RunContext selectRunContext(UUID runId, String providerCode);

    boolean selectBodyFetchRequired(
            RunContext runContext,
            AnnouncementSourceProviderItem item
    );

    PreparedClassification selectClassification(
            RunContext runContext,
            AnnouncementSourceProviderItem item,
            BodySourceCode bodySourceCode,
            BodyAvailabilityCode bodyAvailabilityCode
    );

    boolean selectContentVersionAppendRequired(
            UUID sourceId,
            PreparedClassification preparedClassification
    );

    void saveClassification(
            UUID sourceId,
            UUID runId,
            PreparedClassification preparedClassification,
            String reviewStatusCode
    );

    void saveChangedClassification(
            UUID sourceId,
            UUID runId,
            PreparedClassification preparedClassification,
            String reviewStatusCode,
            int expectedVersion
    );

    record RunContext(
            boolean enabled,
            UUID ruleReleaseId,
            com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet ruleSet,
            AnnouncementSourceSearchPlan searchPlan
    ) {

        public static RunContext disabled() {
            return new RunContext(false, null, null, null);
        }
    }

    record PreparedClassification(
            boolean enabled,
            UUID ruleReleaseId,
            AnnouncementSourceProviderItem item,
            AnnouncementSourceClassificationResult result
    ) {

        public static PreparedClassification disabled(AnnouncementSourceProviderItem item) {
            return new PreparedClassification(false, null, item, null);
        }
    }
}

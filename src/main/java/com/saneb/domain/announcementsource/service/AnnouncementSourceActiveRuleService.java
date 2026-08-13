/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceActiveRuleService.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceDiscoveryTerm;
import java.util.List;
import java.util.UUID;

public interface AnnouncementSourceActiveRuleService {

    ActiveRuleSet selectActiveRuleSet();

    record ActiveRuleSet(
            UUID releaseId,
            AnnouncementSourceClassificationRuleSet ruleSet,
            List<AnnouncementSourceDiscoveryTerm> discoveryTerms
    ) {

        public ActiveRuleSet {
            discoveryTerms = discoveryTerms == null ? List.of() : List.copyOf(discoveryTerms);
        }
    }
}

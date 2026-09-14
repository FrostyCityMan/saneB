package com.saneb.domain.announcementsource.vo;

import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import java.util.UUID;

/** 서버 검증용 일관된 규칙 읽기. persisted hash와 현재 내용으로 다시 계산한 hash를 구분한다. */
public record AnnouncementSourceRuleValidationDetails(UUID releaseId,Integer rowVersion,String releaseStatusCode,
        String persistedSnapshotHash,String calculatedSnapshotHash,AnnouncementSourceClassificationRuleSet ruleSet) { }

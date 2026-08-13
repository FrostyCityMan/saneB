/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceActiveRuleServiceImpl.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.MatchModeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.RuleGroupKindCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.StrengthCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.SupportTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TargetCategoryCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.TermTypeCode;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRule;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationTerm;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceDiscoveryTerm;
import com.saneb.domain.announcementsource.dao.AnnouncementSourceClassificationDao;
import com.saneb.domain.announcementsource.service.AnnouncementSourceActiveRuleService;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceClassificationRuleTermRow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementSourceActiveRuleServiceImpl implements AnnouncementSourceActiveRuleService {

    private final AnnouncementSourceClassificationDao classificationDao;

    public AnnouncementSourceActiveRuleServiceImpl(AnnouncementSourceClassificationDao classificationDao) {
        this.classificationDao = classificationDao;
    }

    @Override
    public ActiveRuleSet selectActiveRuleSet() {
        List<AnnouncementSourceClassificationRuleTermRow> rows =
                classificationDao.selectActiveClassificationRuleTermList();
        if (rows.isEmpty()) {
            throw new ApiException(
                    ErrorCode.ANNOUNCEMENT_SOURCE_RULE_RELEASE_NOT_ACTIVE,
                    HttpStatus.CONFLICT,
                    "적용 중인 공고 분류 규칙 버전이 없습니다."
            );
        }
        AnnouncementSourceClassificationRuleTermRow first = rows.getFirst();
        Map<String, List<AnnouncementSourceClassificationRuleTermRow>> rowsByRule = rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> row.groupCode() + "\u0000" + row.ruleCode(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<AnnouncementSourceClassificationRule> rules = rowsByRule.values().stream()
                .map(this::toRule)
                .toList();
        return new ActiveRuleSet(
                first.releaseId(),
                new AnnouncementSourceClassificationRuleSet(first.releaseCode(), rules),
                rows.stream()
                        .filter(row -> Boolean.TRUE.equals(row.discoveryTerm()))
                        .map(row -> new AnnouncementSourceDiscoveryTerm(
                                row.groupCode(),
                                RuleGroupKindCode.valueOf(row.groupKindCode()),
                                row.termText(),
                                row.discoveryOrder() == null ? Integer.MAX_VALUE : row.discoveryOrder()
                        ))
                        .distinct()
                        .toList()
        );
    }

    private AnnouncementSourceClassificationRule toRule(
            List<AnnouncementSourceClassificationRuleTermRow> ruleRows
    ) {
        AnnouncementSourceClassificationRuleTermRow first = ruleRows.getFirst();
        List<AnnouncementSourceClassificationTerm> terms = ruleRows.stream()
                .map(row -> new AnnouncementSourceClassificationTerm(
                        TermTypeCode.valueOf(row.termTypeCode()),
                        row.termText(),
                        MatchModeCode.valueOf(row.matchModeCode()),
                        Boolean.TRUE.equals(row.classificationTerm()),
                        Boolean.TRUE.equals(row.termEnabled())
                ))
                .toList();
        String canonicalKeyword = ruleRows.stream()
                .filter(row -> "CANONICAL".equals(row.termTypeCode()))
                .map(AnnouncementSourceClassificationRuleTermRow::termText)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorCode.ANNOUNCEMENT_SOURCE_RULE_INVALID,
                        HttpStatus.CONFLICT,
                        "대표 키워드가 없는 공고 분류 규칙이 있습니다."
                ));
        return new AnnouncementSourceClassificationRule(
                first.ruleCode(),
                first.groupCode(),
                RuleGroupKindCode.valueOf(first.groupKindCode()),
                canonicalKeyword,
                StrengthCode.valueOf(first.strengthCode()),
                first.targetCategoryCode() == null
                        ? null : TargetCategoryCode.valueOf(first.targetCategoryCode()),
                first.supportTypeCode() == null
                        ? null : SupportTypeCode.valueOf(first.supportTypeCode()),
                terms,
                Boolean.TRUE.equals(first.ruleEnabled()) && terms.stream().anyMatch(Objects::nonNull)
        );
    }
}

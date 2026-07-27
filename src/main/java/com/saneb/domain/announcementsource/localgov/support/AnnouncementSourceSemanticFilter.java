/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceSemanticFilter.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.support;

import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceSemanticKeywordRuleRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementSourceSemanticFilter {

    /**
     * 출처 정책과 정적 키워드 규칙으로 수집 항목의 검수 대상 여부를 판정합니다.
     *
     * @param source 수집 출처
     * @param title 공고 제목
     * @param rules 활성 키워드 규칙
     * @return 설명 가능한 의미 판정 결과
     */
    public AnnouncementSourceSemanticDecision selectDecision(
            LocalGovernmentNoticeSourceRow source,
            String title,
            List<AnnouncementSourceSemanticKeywordRuleRow> rules
    ) {
        if (!source.semanticallyVerified()
                || "UNVERIFIED".equals(source.sourceBoardTypeCode())
                || "EXCLUDED".equals(source.collectionPolicyCode())) {
            return new AnnouncementSourceSemanticDecision("EXCLUDED", "SOURCE_POLICY_EXCLUDED", null);
        }
        if ("COLLECT_ALL".equals(source.collectionPolicyCode())) {
            return new AnnouncementSourceSemanticDecision("ACCEPTED", "SOURCE_POLICY_COLLECT_ALL", null);
        }

        String normalizedTitle = title == null ? "" : title.toLowerCase(Locale.ROOT);
        List<String> included = selectMatchedKeywords(rules, "INCLUDE", normalizedTitle);
        List<String> excluded = selectMatchedKeywords(rules, "EXCLUDE", normalizedTitle);
        String matchedKeywords = joinKeywords(included, excluded);

        if (included.isEmpty()) {
            return new AnnouncementSourceSemanticDecision(
                    "EXCLUDED",
                    excluded.isEmpty() ? "NO_INCLUDE_KEYWORD" : "EXCLUDE_KEYWORD_MATCHED",
                    matchedKeywords
            );
        }
        if (!excluded.isEmpty()) {
            return new AnnouncementSourceSemanticDecision(
                    "REVIEW_REQUIRED",
                    "INCLUDE_AND_EXCLUDE_KEYWORD",
                    matchedKeywords
            );
        }
        return new AnnouncementSourceSemanticDecision(
                "ACCEPTED",
                "INCLUDE_KEYWORD_MATCHED",
                matchedKeywords
        );
    }

    /**
     * 제목에 포함된 활성 키워드를 규칙 순서대로 반환합니다.
     *
     * @param rules 활성 규칙
     * @param ruleTypeCode 규칙 유형
     * @param normalizedTitle 소문자 제목
     * @return 일치 키워드
     */
    private List<String> selectMatchedKeywords(
            List<AnnouncementSourceSemanticKeywordRuleRow> rules,
            String ruleTypeCode,
            String normalizedTitle
    ) {
        return rules.stream()
                .filter(rule -> ruleTypeCode.equals(rule.ruleTypeCode()))
                .filter(rule -> normalizedTitle.contains(rule.keywordText().toLowerCase(Locale.ROOT)))
                .map(AnnouncementSourceSemanticKeywordRuleRow::keywordText)
                .distinct()
                .toList();
    }

    /**
     * 포함·제외 키워드를 비식별 판정 근거 문자열로 조립합니다.
     *
     * @param included 포함 키워드
     * @param excluded 제외 키워드
     * @return 쉼표로 구분한 키워드 또는 null
     */
    private String joinKeywords(List<String> included, List<String> excluded) {
        String joined = java.util.stream.Stream.concat(included.stream(), excluded.stream())
                .distinct()
                .limit(30)
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? null : joined;
    }
}

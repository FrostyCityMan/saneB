/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceSemanticFilterTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.domain.announcementsource.localgov.vo.AnnouncementSourceSemanticKeywordRuleRow;
import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeSourceRow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementSourceSemanticFilterTest {

    private final AnnouncementSourceSemanticFilter filter = new AnnouncementSourceSemanticFilter();
    private final List<AnnouncementSourceSemanticKeywordRuleRow> rules = List.of(
            new AnnouncementSourceSemanticKeywordRuleRow("INCLUDE_SUPPORT", "INCLUDE", "지원", 10),
            new AnnouncementSourceSemanticKeywordRuleRow("INCLUDE_RECRUITMENT", "INCLUDE", "모집", 20),
            new AnnouncementSourceSemanticKeywordRuleRow("EXCLUDE_PRESS_RELEASE", "EXCLUDE", "보도자료", 10)
    );

    /**
     * 일반 공지 출처에서 지원사업 키워드가 있는 제목만 유효 후보로 분류합니다.
     */
    @Test
    void selectDecisionAcceptsIncludedGeneralNotice() {
        AnnouncementSourceSemanticDecision decision = filter.selectDecision(
                source("GENERAL_NOTICE", "KEYWORD_FILTERED", true),
                "2026년 소상공인 경영 지원사업 모집",
                rules
        );

        assertThat(decision.statusCode()).isEqualTo("ACCEPTED");
        assertThat(decision.reasonCode()).isEqualTo("INCLUDE_KEYWORD_MATCHED");
        assertThat(decision.matchedKeywords()).contains("지원", "모집");
    }

    /**
     * 일반 공지 출처에서 지원사업 키워드가 없는 제목을 검수대기로 저장하지 않도록 제외합니다.
     */
    @Test
    void selectDecisionExcludesIrrelevantGeneralNotice() {
        AnnouncementSourceSemanticDecision decision = filter.selectDecision(
                source("GENERAL_NOTICE", "KEYWORD_FILTERED", true),
                "청사 소방훈련 실시 안내",
                rules
        );

        assertThat(decision.statusCode()).isEqualTo("EXCLUDED");
        assertThat(decision.reasonCode()).isEqualTo("NO_INCLUDE_KEYWORD");
    }

    /**
     * 포함·제외 키워드가 함께 일치하면 자동 제외하지 않고 운영자 확인 대상으로 분류합니다.
     */
    @Test
    void selectDecisionRequiresReviewWhenRulesConflict() {
        AnnouncementSourceSemanticDecision decision = filter.selectDecision(
                source("GENERAL_NOTICE", "KEYWORD_FILTERED", true),
                "소상공인 지원사업 보도자료",
                rules
        );

        assertThat(decision.statusCode()).isEqualTo("REVIEW_REQUIRED");
        assertThat(decision.reasonCode()).isEqualTo("INCLUDE_AND_EXCLUDE_KEYWORD");
    }

    /**
     * 고시·공고 전용 출처의 전체 수집 정책은 제목 키워드와 관계없이 통과시킵니다.
     */
    @Test
    void selectDecisionAcceptsCollectAllPolicy() {
        AnnouncementSourceSemanticDecision decision = filter.selectDecision(
                source("LEGAL_NOTICE", "COLLECT_ALL", true),
                "공고 제2026-100호",
                rules
        );

        assertThat(decision.statusCode()).isEqualTo("ACCEPTED");
        assertThat(decision.reasonCode()).isEqualTo("SOURCE_POLICY_COLLECT_ALL");
    }

    /**
     * 미확인·보도자료·제외 정책 출처는 제목과 관계없이 차단합니다.
     */
    @Test
    void selectDecisionExcludesUnverifiedSource() {
        AnnouncementSourceSemanticDecision decision = filter.selectDecision(
                source("UNVERIFIED", "EXCLUDED", false),
                "소상공인 지원사업 모집",
                rules
        );

        assertThat(decision.statusCode()).isEqualTo("EXCLUDED");
        assertThat(decision.reasonCode()).isEqualTo("SOURCE_POLICY_EXCLUDED");
    }

    /**
     * 의미 판정에 필요한 최소 출처 레코드를 생성합니다.
     *
     * @param boardTypeCode 게시판 종류
     * @param policyCode 수집 정책
     * @param verified 의미 검증 여부
     * @return 테스트 출처
     */
    private LocalGovernmentNoticeSourceRow source(
            String boardTypeCode,
            String policyCode,
            boolean verified
    ) {
        return new LocalGovernmentNoticeSourceRow(
                UUID.randomUUID(), "LGS-TEST", "11", "서울특별시", "110", "테스트구",
                "BASIC_LOCAL_GOVERNMENT", "테스트구청", "https://example.go.kr",
                "https://example.go.kr/notice", null, "official_news_url", "DEFAULT", "GET", null,
                "GENERIC_TABLE", "일반 표형 게시판", null, "HIGH", "VERIFIED",
                boardTypeCode, policyCode, verified, null, null, "테스트 검증",
                false, "READY", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
    }
}

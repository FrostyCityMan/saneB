/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceIdentityNormalizerTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnnouncementSourceIdentityNormalizerTest {

    private final AnnouncementSourceIdentityNormalizer normalizer = new AnnouncementSourceIdentityNormalizer();

    /**
     * 추적 query와 fragment를 제거하고 의미 있는 게시글 식별자는 보존합니다.
     */
    @Test
    void canonicalizeUrlKeepsNoticeIdentityAndRemovesTracking() {
        String result = normalizer.canonicalizeUrl(
                "HTTPS://Example.COM:443/notice/?utm_source=test&bbsNo=87&key=478#detail"
        );

        assertThat(result).isEqualTo("https://example.com/notice?bbsNo=87&key=478");
    }

    /**
     * 제목의 유니코드와 연속 공백을 정규화합니다.
     */
    @Test
    void normalizeTextProducesStableComparisonValue() {
        assertThat(normalizer.normalizeText("  서울   소상공인 지원  "))
                .isEqualTo("서울 소상공인 지원");
    }
}

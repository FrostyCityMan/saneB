/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeUrlValidatorTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saneb.common.error.ApiException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocalGovernmentNoticeUrlValidatorTest {

    private final LocalGovernmentNoticeUrlValidator validator = new LocalGovernmentNoticeUrlValidator();

    /**
     * 내부망, metadata, 인증정보, 비표준 포트 URL을 차단합니다.
     *
     * @param value 차단 대상 URL
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/notices",
            "http://10.0.0.1/notices",
            "http://169.254.169.254/latest/meta-data",
            "http://[::1]/notices",
            "https://user:password@example.com/notices",
            "https://example.com:8443/notices",
            "file:///etc/passwd"
    })
    void validateRejectsUnsafeUrl(String value) {
        assertThatThrownBy(() -> validator.validate(value))
                .isInstanceOf(ApiException.class);
    }
}

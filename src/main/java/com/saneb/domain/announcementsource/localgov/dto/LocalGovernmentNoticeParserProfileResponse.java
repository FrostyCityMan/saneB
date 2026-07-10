/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeParserProfileResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import com.saneb.domain.announcementsource.localgov.vo.LocalGovernmentNoticeParserProfileRow;

public record LocalGovernmentNoticeParserProfileResponse(
        String profileCode,
        String profileName,
        String parserTypeCode,
        boolean enabled
) {

    /**
     * 파서 프로필 조회 결과를 응답으로 변환합니다.
     *
     * @param row 파서 프로필 조회 결과
     * @return 파서 프로필 응답
     */
    public static LocalGovernmentNoticeParserProfileResponse from(LocalGovernmentNoticeParserProfileRow row) {
        return new LocalGovernmentNoticeParserProfileResponse(
                row.profileCode(), row.profileName(), row.parserTypeCode(), row.enabled()
        );
    }
}

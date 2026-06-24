/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuthMeResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthMeResponse(
        UUID userId,
        String loginId,
        String name,
        List<String> roles,
        String primaryRole,
        String defaultRoute,
        boolean passwordResetRequired,
        ProfileResponse profile
) {

    public record ProfileResponse(
            UUID memberProfileId,
            UUID businessProfileId,
            UUID partnerProfileId
    ) {
    }
}

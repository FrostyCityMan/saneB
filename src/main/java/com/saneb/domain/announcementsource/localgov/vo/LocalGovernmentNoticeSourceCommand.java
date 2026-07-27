/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeSourceCommand.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.vo;

import java.util.UUID;

public record LocalGovernmentNoticeSourceCommand(
        UUID sourceId,
        String sidoCode,
        String sidoName,
        String sigunguCode,
        String sigunguName,
        String institutionTypeCode,
        String institutionName,
        String homepageUrl,
        String noticeUrl,
        String collectionEndpointUrl,
        String pageTypeCode,
        String requestProfileCode,
        String parserProfileCode,
        String collectionHint,
        String confidenceCode,
        String validationStatusCode,
        String sourceBoardTypeCode,
        String collectionPolicyCode,
        boolean semanticallyVerified,
        String semanticVerificationNote,
        UUID actorUserId
) {
}

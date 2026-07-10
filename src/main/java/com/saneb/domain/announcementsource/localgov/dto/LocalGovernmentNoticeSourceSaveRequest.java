/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: LocalGovernmentNoticeSourceSaveRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.localgov.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalGovernmentNoticeSourceSaveRequest(
        @Size(max = 10) String sidoCode,
        @NotBlank @Size(max = 80) String sidoName,
        @NotBlank @Size(max = 10) String sigunguCode,
        @NotBlank @Size(max = 100) String sigunguName,
        @NotBlank @Size(max = 40) String institutionTypeCode,
        @NotBlank @Size(max = 150) String institutionName,
        String homepageUrl,
        @NotBlank String noticeUrl,
        @Size(max = 60) String pageTypeCode,
        @Size(max = 50) String parserProfileCode,
        String collectionHint,
        @NotBlank @Size(max = 20) String confidenceCode,
        @NotBlank @Size(max = 30) String validationStatusCode
) {
}

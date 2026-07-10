/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceCollectionRequestCreateRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AnnouncementSourceCollectionRequestCreateRequest(
        @NotBlank(message = "providerCode is required")
        @Size(max = 50, message = "providerCode must be 50 characters or less")
        String providerCode,

        @NotBlank(message = "requestTypeCode is required")
        @Size(max = 30, message = "requestTypeCode must be 30 characters or less")
        String requestTypeCode,

        @Size(max = 100, message = "requestedFrom must be 100 characters or less")
        String requestedFrom,

        @Size(max = 300, message = "searchKeyword must be 300 characters or less")
        String searchKeyword,

        @Size(max = 80, message = "searchRegionCode must be 80 characters or less")
        String searchRegionCode,

        @Size(max = 80, message = "searchCategoryCode must be 80 characters or less")
        String searchCategoryCode,

        LocalDate startDate,

        LocalDate endDate,

        @Min(value = 1, message = "maxCount must be 1 or greater")
        @Max(value = 500, message = "maxCount must be 500 or less")
        Integer maxCount,

        @Size(max = 1000, message = "requestNote must be 1000 characters or less")
        String requestNote
) {
}

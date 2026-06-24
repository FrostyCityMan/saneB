/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: SubscriptionPlanCreateRequest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SubscriptionPlanCreateRequest(
        @NotBlank String planCode,
        @NotBlank String planName,
        @NotBlank String billingCycleCode,
        @NotNull @DecimalMin("0.00") BigDecimal priceAmount,
        String currencyCode,
        Boolean active,
        Integer sortOrder,
        String description
) {
}

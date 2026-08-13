/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceConfirmedClassificationSaveRequest.java
 * 작성자: 김도훈
 */

package com.saneb.domain.announcementsource.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AnnouncementSourceConfirmedClassificationSaveRequest(
        @NotNull(message = "expectedClassificationDecisionId is required")
        UUID expectedClassificationDecisionId,

        @NotNull(message = "expectedVersion is required")
        @PositiveOrZero(message = "expectedVersion must be 0 or greater")
        Integer expectedVersion,

        @NotEmpty(message = "targetCategoryCodes must contain at least one value")
        @Size(max = 5)
        List<String> targetCategoryCodes,

        @NotEmpty(message = "supportTypeCodes must contain at least one value")
        @Size(max = 7)
        List<String> supportTypeCodes,

        @Size(max = 1000, message = "reviewNote must be 1000 characters or less")
        String reviewNote
) {
}

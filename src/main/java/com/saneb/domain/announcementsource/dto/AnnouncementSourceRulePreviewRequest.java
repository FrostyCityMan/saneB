package com.saneb.domain.announcementsource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 저장 없이 지정 release로 제목과 본문을 판정하는 요청입니다. */
public record AnnouncementSourceRulePreviewRequest(
        @Size(max = 50, message = "providerCode must be 50 characters or less")
        String providerCode,
        @NotBlank(message = "title is required")
        @Size(max = 500, message = "title must be 500 characters or less")
        String title,
        @Size(max = 200000, message = "bodyText is too long")
        String bodyText,
        @Size(max = 300, message = "agencyName must be 300 characters or less")
        String agencyName,
        @Valid
        @Size(max = 30, message = "agencyAliases must contain 30 values or less")
        List<
                @NotBlank(message = "agencyAlias must not be blank")
                @Size(max = 300, message = "agencyAlias must be 300 characters or less") String
                > agencyAliases,
        @NotNull(message = "expectedVersion is required")
        @PositiveOrZero(message = "expectedVersion must be 0 or greater")
        Integer expectedVersion
) {
}

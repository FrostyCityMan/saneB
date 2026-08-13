package com.saneb.domain.announcementsource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DRAFT 키워드 규칙과 유의어를 등록하거나 수정하는 요청입니다. */
public record AnnouncementSourceKeywordRuleSaveRequest(
        @NotBlank(message = "ruleGroupCode is required")
        @Size(max = 80, message = "ruleGroupCode must be 80 characters or less")
        String ruleGroupCode,
        @NotBlank(message = "canonicalKeyword is required")
        @Size(max = 200, message = "canonicalKeyword must be 200 characters or less")
        String canonicalKeyword,
        @Valid
        @Size(max = 50, message = "synonyms must contain 50 values or less")
        List<
                @NotBlank(message = "synonym must not be blank")
                @Size(max = 200, message = "synonym must be 200 characters or less") String
                > synonyms,
        @NotBlank(message = "strengthCode is required")
        String strengthCode,
        @NotBlank(message = "matchModeCode is required")
        String matchModeCode,
        @NotNull(message = "sortOrder is required")
        @PositiveOrZero(message = "sortOrder must be 0 or greater")
        Integer sortOrder,
        Boolean discoveryTerm,
        @Positive(message = "discoveryOrder must be greater than 0")
        Integer discoveryOrder,
        @NotNull(message = "expectedVersion is required")
        @PositiveOrZero(message = "expectedVersion must be 0 or greater")
        Integer expectedVersion,
        @NotBlank(message = "changeReason is required")
        @Size(max = 1000, message = "changeReason must be 1000 characters or less")
        String changeReason
) {
}

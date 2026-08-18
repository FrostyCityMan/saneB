package com.saneb.domain.announcementsource.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationRuleSet;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleDeleteRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleSaveRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleStatusUpdateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceKeywordRuleSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePreviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePreviewResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRulePublicationResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleReleaseCreateRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleReleaseSummaryResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

/** 공고 분류 규칙 release 관리 계약입니다. */
public interface AnnouncementSourceRuleReleaseService {

    PageResponse<AnnouncementSourceRuleReleaseSummaryResponse> selectRuleReleaseList(
            String releaseStatusCode,
            int page,
            int size
    );

    AnnouncementSourceRuleReleaseSummaryResponse insertRuleReleaseDraft(
            Authentication authentication,
            AnnouncementSourceRuleReleaseCreateRequest request
    );

    PageResponse<AnnouncementSourceKeywordRuleSummaryResponse> selectKeywordRuleList(
            UUID releaseId,
            String groupKindCode,
            String groupCode,
            String strengthCode,
            String matchModeCode,
            Boolean enabled,
            String keyword,
            int page,
            int size
    );

    AnnouncementSourceKeywordRuleSummaryResponse insertKeywordRule(
            Authentication authentication,
            UUID releaseId,
            AnnouncementSourceKeywordRuleSaveRequest request
    );

    AnnouncementSourceKeywordRuleSummaryResponse updateKeywordRule(
            Authentication authentication,
            UUID releaseId,
            UUID ruleId,
            AnnouncementSourceKeywordRuleSaveRequest request
    );

    AnnouncementSourceKeywordRuleSummaryResponse updateKeywordRuleStatus(
            Authentication authentication,
            UUID releaseId,
            UUID ruleId,
            AnnouncementSourceKeywordRuleStatusUpdateRequest request
    );

    void deleteKeywordRule(
            Authentication authentication,
            UUID releaseId,
            UUID ruleId,
            AnnouncementSourceKeywordRuleDeleteRequest request
    );

    AnnouncementSourceRulePreviewResponse selectPreview(
            UUID releaseId,
            AnnouncementSourceRulePreviewRequest request
    );

    AnnouncementSourceRulePublicationResponse updateRuleReleasePublication(
            Authentication authentication,
            UUID releaseId,
            AnnouncementSourceRulePublicationRequest request
    );

    AnnouncementSourceClassificationRuleSet selectActiveRuleSet(UUID releaseId);

    AnnouncementSourceClassificationRuleSet selectPublishedRuleSet(UUID releaseId);

    AnnouncementSourceRuleGoldenSetRunResponse insertGoldenSetRun(
            Authentication authentication,
            UUID releaseId,
            AnnouncementSourceRuleGoldenSetRunRequest request
    );
}

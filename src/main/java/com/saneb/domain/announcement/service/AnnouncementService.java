package com.saneb.domain.announcement.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcement.dto.AnnouncementConditionsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementDetailsResponse;
import com.saneb.domain.announcement.dto.AnnouncementManualStatusUpdateRequest;
import com.saneb.domain.announcement.dto.AnnouncementSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementStepsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementSummaryResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementService {

    PageResponse<AnnouncementSummaryResponse> selectAnnouncementList(
            String keyword,
            String targetTypeCode,
            String manualStatusCode,
            String approvalStatusCode,
            int page,
            int size
    );

    AnnouncementDetailsResponse insertAnnouncement(Authentication authentication, AnnouncementSaveRequest request);

    AnnouncementDetailsResponse selectAnnouncementDetails(UUID announcementId);

    AnnouncementDetailsResponse updateAnnouncement(
            Authentication authentication,
            UUID announcementId,
            AnnouncementSaveRequest request
    );

    void updateAnnouncementConditions(
            Authentication authentication,
            UUID announcementId,
            AnnouncementConditionsSaveRequest request
    );

    void updateAnnouncementSteps(
            Authentication authentication,
            UUID announcementId,
            AnnouncementStepsSaveRequest request
    );

    void updateAnnouncementManualStatus(
            Authentication authentication,
            UUID announcementId,
            AnnouncementManualStatusUpdateRequest request
    );
}

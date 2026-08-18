package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunActionRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunPreviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunStatusRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementSourceReclassificationRunService {

    AnnouncementSourceReclassificationRunResponse insertPreviewRun(
            Authentication authentication,
            AnnouncementSourceReclassificationRunPreviewRequest request
    );

    List<AnnouncementSourceReclassificationRunResponse> selectRunList();

    AnnouncementSourceReclassificationRunResponse selectRunDetails(UUID runId);

    AnnouncementSourceReclassificationRunResponse updateApplicationStarted(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunActionRequest request
    );

    AnnouncementSourceReclassificationRunResponse updateApplicationPaused(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunStatusRequest request
    );

    AnnouncementSourceReclassificationRunResponse updateApplicationResumed(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunStatusRequest request
    );

    AnnouncementSourceReclassificationRunResponse updateRollbackStarted(
            Authentication authentication,
            UUID runId,
            AnnouncementSourceReclassificationRunActionRequest request
    );

    void insertNextRunBatch();
}

package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunActionRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunPreviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunStatusRequest;
import com.saneb.domain.announcementsource.service.AnnouncementSourceReclassificationRunService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/announcement-source-reclassification-runs")
public class AnnouncementSourceReclassificationRunController {

    private final AnnouncementSourceReclassificationRunService service;

    public AnnouncementSourceReclassificationRunController(AnnouncementSourceReclassificationRunService service) {
        this.service = service;
    }

    @PostMapping("/previews")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceReclassificationRunResponse> insertPreviewRun(
            Authentication authentication,
            @Valid @RequestBody AnnouncementSourceReclassificationRunPreviewRequest request
    ) {
        return ApiResponse.success(service.insertPreviewRun(authentication, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<List<AnnouncementSourceReclassificationRunResponse>> selectRunList() {
        return ApiResponse.success(service.selectRunList());
    }

    @GetMapping("/{runId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<AnnouncementSourceReclassificationRunResponse> selectRunDetails(@PathVariable UUID runId) {
        return ApiResponse.success(service.selectRunDetails(runId));
    }

    @PostMapping("/{runId}/application")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceReclassificationRunResponse> updateApplicationStarted(
            Authentication authentication,
            @PathVariable UUID runId,
            @Valid @RequestBody AnnouncementSourceReclassificationRunActionRequest request
    ) {
        return ApiResponse.success(service.updateApplicationStarted(authentication, runId, request));
    }

    @PostMapping("/{runId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceReclassificationRunResponse> updateApplicationPaused(
            Authentication authentication,
            @PathVariable UUID runId,
            @Valid @RequestBody AnnouncementSourceReclassificationRunStatusRequest request
    ) {
        return ApiResponse.success(service.updateApplicationPaused(authentication, runId, request));
    }

    @PostMapping("/{runId}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceReclassificationRunResponse> updateApplicationResumed(
            Authentication authentication,
            @PathVariable UUID runId,
            @Valid @RequestBody AnnouncementSourceReclassificationRunStatusRequest request
    ) {
        return ApiResponse.success(service.updateApplicationResumed(authentication, runId, request));
    }

    @PostMapping("/{runId}/rollback")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnnouncementSourceReclassificationRunResponse> updateRollbackStarted(
            Authentication authentication,
            @PathVariable UUID runId,
            @Valid @RequestBody AnnouncementSourceReclassificationRunActionRequest request
    ) {
        return ApiResponse.success(service.updateRollbackStarted(authentication, runId, request));
    }
}

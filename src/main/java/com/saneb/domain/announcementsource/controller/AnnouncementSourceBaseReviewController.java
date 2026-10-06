package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceBaseReviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceBaseReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/base-review")
public class AnnouncementSourceBaseReviewController {
    private final AnnouncementSourceBaseReviewService service;
    public AnnouncementSourceBaseReviewController(AnnouncementSourceBaseReviewService service) { this.service = service; }
    @PostMapping("/announcements") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ApiResponse<AnnouncementSourceLinkResponse> insertReviewedAnnouncement(Authentication authentication,
            @PathVariable UUID sourceId, @Valid @RequestBody AnnouncementSourceBaseReviewRequest request) {
        return ApiResponse.success(service.insertReviewedAnnouncement(authentication, sourceId, request));
    }
}

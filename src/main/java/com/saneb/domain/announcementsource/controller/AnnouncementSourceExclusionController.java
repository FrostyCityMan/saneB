package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceExclusionSummaryResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceExclusionService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/admin/announcement-source-exclusions")
public class AnnouncementSourceExclusionController {
    private final AnnouncementSourceExclusionService service;
    public AnnouncementSourceExclusionController(AnnouncementSourceExclusionService service) { this.service = service; }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'APPROVER')")
    public ResponseEntity<ApiResponse<AnnouncementSourceExclusionSummaryResponse>> selectSummaryDetails() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectSummaryDetails()));
    }
}

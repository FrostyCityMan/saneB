package com.saneb.domain.announcement.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcement.dto.AnnouncementConditionsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementDetailsResponse;
import com.saneb.domain.announcement.dto.AnnouncementManualStatusUpdateRequest;
import com.saneb.domain.announcement.dto.AnnouncementSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementStepsSaveRequest;
import com.saneb.domain.announcement.dto.AnnouncementSummaryResponse;
import com.saneb.domain.announcement.service.AnnouncementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

	private final AnnouncementService announcementService;

	public AnnouncementController(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	@GetMapping
	public ApiResponse<PageResponse<AnnouncementSummaryResponse>> selectAnnouncementList(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String targetTypeCode,
			@RequestParam(required = false) String manualStatusCode,
			@RequestParam(required = false) String approvalStatusCode,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(announcementService.selectAnnouncementList(
				keyword,
				targetTypeCode,
				manualStatusCode,
				approvalStatusCode,
				page,
				size
		));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
	public ApiResponse<AnnouncementDetailsResponse> insertAnnouncement(
			Authentication authentication,
			@Valid @RequestBody AnnouncementSaveRequest request
	) {
		return ApiResponse.success(announcementService.insertAnnouncement(authentication, request));
	}

	@GetMapping("/{announcementId}")
	public ApiResponse<AnnouncementDetailsResponse> selectAnnouncementDetails(
			@PathVariable UUID announcementId
	) {
		return ApiResponse.success(announcementService.selectAnnouncementDetails(announcementId));
	}

	@PutMapping("/{announcementId}")
	@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
	public ApiResponse<AnnouncementDetailsResponse> updateAnnouncement(
			Authentication authentication,
			@PathVariable UUID announcementId,
			@Valid @RequestBody AnnouncementSaveRequest request
	) {
		return ApiResponse.success(announcementService.updateAnnouncement(authentication, announcementId, request));
	}

	@PutMapping("/{announcementId}/conditions")
	@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
	public ApiResponse<Void> updateAnnouncementConditions(
			Authentication authentication,
			@PathVariable UUID announcementId,
			@Valid @RequestBody AnnouncementConditionsSaveRequest request
	) {
		announcementService.updateAnnouncementConditions(authentication, announcementId, request);
		return ApiResponse.success(null);
	}

	@PutMapping("/{announcementId}/steps")
	@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
	public ApiResponse<Void> updateAnnouncementSteps(
			Authentication authentication,
			@PathVariable UUID announcementId,
			@Valid @RequestBody AnnouncementStepsSaveRequest request
	) {
		announcementService.updateAnnouncementSteps(authentication, announcementId, request);
		return ApiResponse.success(null);
	}

	@PatchMapping("/{announcementId}/manual-status")
	@PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
	public ApiResponse<Void> updateAnnouncementManualStatus(
			Authentication authentication,
			@PathVariable UUID announcementId,
			@Valid @RequestBody AnnouncementManualStatusUpdateRequest request
	) {
		announcementService.updateAnnouncementManualStatus(authentication, announcementId, request);
		return ApiResponse.success(null);
	}
}

package com.saneb.domain.applicationprogress.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressDetailsResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressStartRequest;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressSummaryResponse;
import com.saneb.domain.applicationprogress.dto.ProgressActionRequest;
import com.saneb.domain.applicationprogress.dto.ProgressChecklistSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressReceiptSaveRequest;
import com.saneb.domain.applicationprogress.dto.ProgressResultSaveRequest;
import com.saneb.domain.applicationprogress.service.ApplicationProgressService;
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
@RequestMapping("/api/v1/application-progresses")
public class ApplicationProgressController {

    private final ApplicationProgressService applicationProgressService;

    public ApplicationProgressController(ApplicationProgressService applicationProgressService) {
        this.applicationProgressService = applicationProgressService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<ApplicationProgressDetailsResponse> insertApplicationProgress(
            Authentication authentication,
            @Valid @RequestBody ApplicationProgressStartRequest request
    ) {
        return ApiResponse.success(applicationProgressService.insertApplicationProgress(authentication, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<PageResponse<ApplicationProgressSummaryResponse>> selectApplicationProgressList(
            Authentication authentication,
            @RequestParam(required = false) UUID announcementId,
            @RequestParam(required = false) UUID memberUserId,
            @RequestParam(required = false) UUID matchingCaseId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(applicationProgressService.selectApplicationProgressList(
                authentication,
                announcementId,
                memberUserId,
                matchingCaseId,
                statusCode,
                page,
                size
        ));
    }

    @GetMapping("/{progressId}")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN')")
    public ApiResponse<ApplicationProgressDetailsResponse> selectApplicationProgressDetails(
            @PathVariable UUID progressId
    ) {
        return ApiResponse.success(applicationProgressService.selectApplicationProgressDetails(progressId));
    }

    @PatchMapping("/{progressId}/steps/{stepId}/action")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ApplicationProgressDetailsResponse> updateProgressStepAction(
            Authentication authentication,
            @PathVariable UUID progressId,
            @PathVariable UUID stepId,
            @Valid @RequestBody ProgressActionRequest request
    ) {
        return ApiResponse.success(applicationProgressService.updateProgressStepAction(
                authentication,
                progressId,
                stepId,
                request
        ));
    }

    @PutMapping("/{progressId}/steps/{stepId}/documents")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ApplicationProgressDetailsResponse> saveProgressStepDocuments(
            Authentication authentication,
            @PathVariable UUID progressId,
            @PathVariable UUID stepId,
            @Valid @RequestBody ProgressChecklistSaveRequest request
    ) {
        return ApiResponse.success(applicationProgressService.saveProgressStepDocuments(
                authentication,
                progressId,
                stepId,
                request
        ));
    }

    @PatchMapping("/{progressId}/receipt")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ApplicationProgressDetailsResponse> updateProgressReceipt(
            Authentication authentication,
            @PathVariable UUID progressId,
            @Valid @RequestBody ProgressReceiptSaveRequest request
    ) {
        return ApiResponse.success(applicationProgressService.updateProgressReceipt(authentication, progressId, request));
    }

    @PatchMapping("/{progressId}/result")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<ApplicationProgressDetailsResponse> updateProgressResult(
            Authentication authentication,
            @PathVariable UUID progressId,
            @Valid @RequestBody ProgressResultSaveRequest request
    ) {
        return ApiResponse.success(applicationProgressService.updateProgressResult(authentication, progressId, request));
    }
}

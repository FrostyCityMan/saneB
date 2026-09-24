package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentSegmentAnalysisResponse;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentSegmentService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/attachment-extractions/{extractionId}/segment-analysis")
public class AnnouncementAttachmentSegmentController {
    private final AnnouncementAttachmentSegmentService service;
    public AnnouncementAttachmentSegmentController(AnnouncementAttachmentSegmentService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentSegmentAnalysisResponse>> selectAnalysisDetails(@PathVariable UUID sourceId, @PathVariable UUID extractionId,
            @RequestParam(required=false) String analysisVersion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(analysisVersion == null
                ? service.selectAnalysisDetails(sourceId, extractionId) : service.selectAnalysisDetails(sourceId, extractionId, analysisVersion)));
    }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<AttachmentSegmentAnalysisResponse>> insertAnalysis(Authentication authentication, @PathVariable UUID sourceId, @PathVariable UUID extractionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertAnalysis(authentication, sourceId, extractionId)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifier() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED), "원문 ID와 첨부 추출 ID는 UUID 형식이어야 합니다."));
    }
}

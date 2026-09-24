package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ApiException;
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
    @GetMapping("/evaluations/{evaluationId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentSegmentAnalysisResponse.EvaluationBinding>> selectEvaluationAnalysisDetails(
            @PathVariable UUID sourceId, @PathVariable UUID extractionId, @PathVariable UUID evaluationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(
                service.selectEvaluationAnalysisDetails(sourceId, extractionId, evaluationId)));
    }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<AttachmentSegmentAnalysisResponse>> insertAnalysis(Authentication authentication, @PathVariable UUID sourceId, @PathVariable UUID extractionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertAnalysis(authentication, sourceId, extractionId)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifier(MethodArgumentTypeMismatchException exception) {
        String message = "evaluationId".equals(exception.getName()) ? "판정 ID는 UUID 형식이어야 합니다. 분류 이력에서 판정을 다시 선택하세요."
                : "원문 ID와 첨부 추출 ID는 UUID 형식이어야 합니다.";
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED), message));
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAnalysisError(ApiException exception) {
        return ResponseEntity.status(exception.httpStatus()).cacheControl(CacheControl.noStore())
                .body(ApiResponse.failure(ErrorResponse.of(exception.errorCode()), exception.getMessage()));
    }
}

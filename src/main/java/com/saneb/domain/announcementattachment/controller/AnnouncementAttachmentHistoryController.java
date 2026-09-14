package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentHistoryResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentHistoryService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/attachment-classification")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
public class AnnouncementAttachmentHistoryController {
    private final AnnouncementAttachmentHistoryService service;
    public AnnouncementAttachmentHistoryController(AnnouncementAttachmentHistoryService service) { this.service=service; }
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentHistoryResponses.Summary>>> selectEvaluationList(@PathVariable UUID sourceId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectEvaluationList(sourceId,page,size)));
    }
    @GetMapping("/{evaluationId}")
    public ResponseEntity<ApiResponse<AttachmentHistoryResponses.Details>> selectEvaluationDetails(@PathVariable UUID sourceId,@PathVariable UUID evaluationId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectEvaluationDetails(sourceId,evaluationId)));
    }
    @GetMapping("/{evaluationId}/inputs")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentHistoryResponses.Input>>> selectInputList(@PathVariable UUID sourceId,@PathVariable UUID evaluationId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectInputList(sourceId,evaluationId,page,size)));
    }
    @GetMapping("/{evaluationId}/matches")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentHistoryResponses.Match>>> selectMatchList(@PathVariable UUID sourceId,@PathVariable UUID evaluationId,
            @RequestParam(required=false) UUID fileId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectMatchList(sourceId,evaluationId,fileId,page,size)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleParameterTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                UUID.class.equals(exception.getRequiredType()) ? "원문·판정·파일 식별자는 UUID 형식이어야 합니다." : "페이지(page)와 페이지 크기(size)는 정수여야 합니다."));
    }
}

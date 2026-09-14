package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentSourceResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCurrentService;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'APPROVER')")
public class AnnouncementAttachmentCurrentController {
    private final AnnouncementAttachmentCurrentService service;
    public AnnouncementAttachmentCurrentController(AnnouncementAttachmentCurrentService service) { this.service=service; }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AttachmentSourceResponses.Summary>>> selectSourceList(
            @RequestParam(required=false) String providerCode, @RequestParam(required=false) String effectiveStatusCode,
            @RequestParam(required=false) String jobStatusCode, @RequestParam(required=false) String targetCategoryCode,
            @RequestParam(required=false) String supportTypeCode, @RequestParam(required=false) String keyword,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate collectedFrom,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate collectedTo,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String processingFlowStatusCode) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectSourceList(
                new AttachmentSourceSearchCondition(providerCode,effectiveStatusCode,jobStatusCode,targetCategoryCode,
                        supportTypeCode,keyword,collectedFrom,collectedTo,page,size,processingFlowStatusCode))));
    }
    @GetMapping("/{sourceId}")
    public ResponseEntity<ApiResponse<AttachmentSourceResponses.Details>> selectSourceDetails(@PathVariable UUID sourceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectSourceDetails(sourceId)));
    }
    @GetMapping("/{sourceId}/attachment-classification")
    public ResponseEntity<ApiResponse<AttachmentSourceResponses.Summary>> selectClassificationDetails(@PathVariable UUID sourceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectClassificationDetails(sourceId)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleParameterTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message=UUID.class.equals(exception.getRequiredType()) ? "원문 식별자(sourceId)는 UUID 형식이어야 합니다."
                : LocalDate.class.equals(exception.getRequiredType()) ? "수집일은 실제 달력의 YYYY-MM-DD 형식이어야 합니다."
                : "페이지(page)와 페이지 크기(size)는 정수여야 합니다.";
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),message));
    }
}

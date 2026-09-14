package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentRetryRequest;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRetryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/attachment-jobs")
public class AnnouncementAttachmentRetryController {
    private final AnnouncementAttachmentRetryService service;
    public AnnouncementAttachmentRetryController(AnnouncementAttachmentRetryService service) { this.service=service; }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AttachmentJobResponse>> insertFileRetry(Authentication authentication,@PathVariable UUID sourceId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentRetryRequest request) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertFileRetry(authentication,sourceId,key,request)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifiers(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "원문 식별자와 UUID 형식의 Idempotency-Key 헤더가 필요합니다."));
    }
}

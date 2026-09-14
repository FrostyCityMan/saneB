package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentCollectionService;
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
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}")
public class AnnouncementAttachmentCollectionController {
    private final AnnouncementAttachmentCollectionService service;
    public AnnouncementAttachmentCollectionController(AnnouncementAttachmentCollectionService service) { this.service=service; }
    @GetMapping("/attachment-collection-context") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentCollectionContext>> selectCollectionContextDetails(Authentication authentication,@PathVariable UUID sourceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectCollectionContextDetails(authentication,sourceId)));
    }
    @PostMapping("/attachment-jobs/collection") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<AttachmentJobResponse>> insertCollectionJob(Authentication authentication,@PathVariable UUID sourceId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentCollectionRequests.Request request) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertCollectionJob(authentication,sourceId,key,request)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifiers(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "원문 식별자와 수집 예약의 UUID 형식 Idempotency-Key 헤더가 필요합니다."));
    }
}

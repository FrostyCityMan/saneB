package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReviewService;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/attachment-classification")
public class AnnouncementAttachmentReviewController {
    private final AnnouncementAttachmentReviewService service;
    public AnnouncementAttachmentReviewController(AnnouncementAttachmentReviewService service) { this.service=service; }

    @GetMapping("/review-context")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentReviewResponses.Context>> selectReviewContextDetails(@PathVariable UUID sourceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectReviewContextDetails(sourceId)));
    }
    @PostMapping("/confirmations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AttachmentReviewResponses.Confirmation>> insertConfirmation(Authentication authentication,
            @PathVariable UUID sourceId, @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AttachmentReviewRequests.Confirmation request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertConfirmation(authentication,sourceId,key,request)));
    }
    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AnnouncementSourceLinkResponse>> insertOperationalAnnouncement(Authentication authentication,
            @PathVariable UUID sourceId, @Valid @RequestBody AttachmentReviewRequests.Conversion request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertOperationalAnnouncement(authentication,sourceId,request)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifierValidation(Exception exception) {
        String message=exception instanceof MissingRequestHeaderException ? "검수 확인에는 UUID 형식의 Idempotency-Key 헤더가 필요합니다."
                : "원문 식별자(sourceId)와 Idempotency-Key는 UUID 형식이어야 합니다.";
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),message));
    }
}

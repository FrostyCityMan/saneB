package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentLinkedBatchRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentLinkedBatchResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/admin/announcement-attachment-linked-evidence-batches")
public class AnnouncementAttachmentLinkedBatchController {
    private final AnnouncementAttachmentBatchService service;
    public AnnouncementAttachmentLinkedBatchController(AnnouncementAttachmentBatchService service) {this.service=service;}
    @PostMapping("/scope-preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentLinkedBatchResponses.Preview>> selectScopePreview(Authentication actor,
            @Valid @RequestBody AttachmentLinkedBatchRequests.Scope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectLinkedScopePreview(actor,scope)));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<com.saneb.common.error.ErrorResponse>> handleInput() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(
                com.saneb.common.error.ErrorResponse.of(com.saneb.common.error.ErrorCode.VALIDATION_FAILED),
                "정책 ID·원문 ID는 UUID, 다운로드 상한은 정수로 입력하세요. URL·파서·실행 상태 등 정의하지 않은 필드는 지정할 수 없습니다."));
    }
}

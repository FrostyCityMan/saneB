package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchResponses;
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
    @PutMapping("/{batchId}/collection") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> updateCollectionStart(Authentication actor,
            @PathVariable java.util.UUID batchId,@Valid @RequestBody AttachmentBatchRequests.Collection request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.updateLinkedCollectionStart(actor,batchId,request)));
    }
    @PutMapping("/{batchId}/collection-pause") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> updateCollectionPause(Authentication actor,
            @PathVariable java.util.UUID batchId,@Valid @RequestBody AttachmentBatchRequests.Pause request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.updateLinkedCollectionPause(actor,batchId,request)));
    }
    @PutMapping("/{batchId}/collection-resume") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> updateCollectionResume(Authentication actor,
            @PathVariable java.util.UUID batchId,@Valid @RequestBody AttachmentBatchRequests.Collection request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.updateLinkedCollectionResume(actor,batchId,request)));
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.saneb.domain.announcementattachment.dto.AttachmentBatchResponses.Batch>> insertBatch(Authentication actor,
            @RequestHeader("Idempotency-Key") java.util.UUID key,@Valid @RequestBody AttachmentLinkedBatchRequests.Reservation request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(service.insertLinkedBatch(actor,key,request)));
    }
    @PostMapping("/scope-preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentLinkedBatchResponses.Preview>> selectScopePreview(Authentication actor,
            @Valid @RequestBody AttachmentLinkedBatchRequests.Scope scope) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectLinkedScopePreview(actor,scope)));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<com.saneb.common.error.ErrorResponse>> handleInput() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(
                com.saneb.common.error.ErrorResponse.of(com.saneb.common.error.ErrorCode.VALIDATION_FAILED),
                "정책 ID·원문 ID·Idempotency-Key는 UUID, 다운로드 상한은 정수로 입력하세요. 예약에는 멱등 키가 필요하며 URL·파서·실행 상태 등 정의하지 않은 필드는 지정할 수 없습니다."));
    }
}

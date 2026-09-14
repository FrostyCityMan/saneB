package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchPreviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-attachment-batches/{batchId}/classification-preview")
public class AnnouncementAttachmentBatchPreviewController {
    private final AnnouncementAttachmentBatchPreviewService service;
    public AnnouncementAttachmentBatchPreviewController(AnnouncementAttachmentBatchPreviewService service) {this.service=service;}
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchPreviewResponses.Preview>> insertPreview(Authentication actor,@PathVariable UUID batchId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentBatchPreviewRequests.Preparation request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertPreview(actor,batchId,key,request)));
    }
    @PutMapping("/selection") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchPreviewResponses.Preview>> updateSelection(Authentication actor,@PathVariable UUID batchId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentBatchPreviewRequests.Selection request) {return response(service.updateSelection(actor,batchId,key,request));}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBatchPreviewResponses.Preview>> selectCurrentPreviewDetails(Authentication actor,@PathVariable UUID batchId) {return response(service.selectCurrentPreviewDetails(actor,batchId));}
    @GetMapping("/{previewId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBatchPreviewResponses.Preview>> selectPreviewDetails(Authentication actor,@PathVariable UUID batchId,@PathVariable UUID previewId) {return response(service.selectPreviewDetails(actor,batchId,previewId));}
    @GetMapping("/{previewId}/items") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentBatchPreviewResponses.Item>>> selectItemList(Authentication actor,@PathVariable UUID batchId,@PathVariable UUID previewId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return response(service.selectItemList(actor,batchId,previewId,page,size));}
    private <T> ResponseEntity<ApiResponse<T>> response(T value) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(value));}
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "배치·미리보기·작업 ID와 멱등 키는 UUID여야 합니다. 현재 버전·조회한 지문·사유와 선택 작업 목록만 입력하세요. URL·파일·성공 판정·정책 변경은 지정할 수 없습니다."));
    }
}

package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchRollbackResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchRollbackService;
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
@RequestMapping("/api/v2/admin/announcement-attachment-batches/{batchId}/rollback")
public class AnnouncementAttachmentBatchRollbackController {
    private final AnnouncementAttachmentBatchRollbackService service;
    public AnnouncementAttachmentBatchRollbackController(AnnouncementAttachmentBatchRollbackService service){this.service=service;}
    @GetMapping("/preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<Preview>> selectPreviewDetails(Authentication actor,@PathVariable UUID batchId){return ok(service.selectPreviewDetails(actor,batchId));}
    @GetMapping("/items") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<Item>>> selectItemList(Authentication actor,@PathVariable UUID batchId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return ok(service.selectItemList(actor,batchId,page,size));}
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Receipt>> insertRollback(Authentication actor,@PathVariable UUID batchId,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentBatchRollbackRequest request){return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertRollback(actor,batchId,key,request)));}
    @GetMapping("/actions/{actionId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<Receipt>> selectActionDetails(Authentication actor,@PathVariable UUID batchId,@PathVariable UUID actionId){return ok(service.selectActionDetails(actor,batchId,actionId));}
    private <T> ResponseEntity<ApiResponse<T>> ok(T data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(data));}
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception ignored){return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),"배치·승인 ID·멱등 키는 UUID여야 합니다. 최신 원복 미리보기의 버전·지문·전체/대상/삭제/영향/취소 건수와 확인·사유를 입력하세요."));}
}

package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentNormalRollbackService;
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
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/attachment-jobs/{jobId}/rollback")
public class AnnouncementAttachmentNormalRollbackController {
    private final AnnouncementAttachmentNormalRollbackService service;
    public AnnouncementAttachmentNormalRollbackController(AnnouncementAttachmentNormalRollbackService service){this.service=service;}
    @GetMapping("/preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<Preview>> selectPreviewDetails(Authentication actor,@PathVariable UUID sourceId,@PathVariable UUID jobId){return ok(service.selectPreviewDetails(actor,sourceId,jobId));}
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Receipt>> insertRollback(Authentication actor,@PathVariable UUID sourceId,@PathVariable UUID jobId,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentNormalRollbackRequest request){return ok(service.insertRollback(actor,sourceId,jobId,key,request));}
    @GetMapping("/actions/{actionId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<Receipt>> selectActionDetails(Authentication actor,@PathVariable UUID sourceId,@PathVariable UUID jobId,@PathVariable UUID actionId){return ok(service.selectActionDetails(actor,sourceId,jobId,actionId));}
    private <T> ResponseEntity<ApiResponse<T>> ok(T data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(data));}
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception ignored){return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),"원문·작업·영수증·멱등 키는 UUID여야 합니다. 현재 버전·원복 지문·기본 경로 및 검수 복구 효과·확인·사유를 입력하세요."));}
}

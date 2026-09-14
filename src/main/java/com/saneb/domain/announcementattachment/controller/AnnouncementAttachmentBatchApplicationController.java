package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchApplicationService;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchApplicationRows;
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
@RequestMapping("/api/v2/admin/announcement-attachment-batches/{batchId}/application")
public class AnnouncementAttachmentBatchApplicationController {
    private final AnnouncementAttachmentBatchApplicationService service;
    public AnnouncementAttachmentBatchApplicationController(AnnouncementAttachmentBatchApplicationService service) {this.service=service;}
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchApplicationResponse>> insertApplication(Authentication actor,@PathVariable UUID batchId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentBatchApplicationRequest request) {return accepted(service.insertAction(actor,batchId,key,"START",request));}
    @PostMapping("/pause") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchApplicationResponse>> insertPause(Authentication actor,@PathVariable UUID batchId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentBatchApplicationRequest request) {return accepted(service.insertAction(actor,batchId,key,"PAUSE",request));}
    @PostMapping("/resume") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchApplicationResponse>> insertResume(Authentication actor,@PathVariable UUID batchId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentBatchApplicationRequest request) {return accepted(service.insertAction(actor,batchId,key,"RESUME",request));}
    @GetMapping("/actions/{actionId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBatchApplicationResponse>> selectActionDetails(Authentication actor,@PathVariable UUID batchId,@PathVariable UUID actionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectActionDetails(actor,batchId,actionId)));
    }
    @GetMapping("/items") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentBatchApplicationRows.Item>>> selectItemList(Authentication actor,@PathVariable UUID batchId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectItemList(actor,batchId,page,size)));
    }
    private <T> ResponseEntity<ApiResponse<T>> accepted(T result) {return ResponseEntity.status(HttpStatus.ACCEPTED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(result));}
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception ignored) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "배치·미리보기·요청 ID와 멱등 키는 UUID여야 합니다. 조회한 버전·미리보기 지문·전체/선택/삭제 건수, 재검수 확인과 사유만 입력하세요."));
    }
}

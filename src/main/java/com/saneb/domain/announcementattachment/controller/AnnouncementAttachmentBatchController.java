package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchRows;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchService;
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
@RequestMapping("/api/v2/admin/announcement-attachment-batches")
public class AnnouncementAttachmentBatchController {
    private final AnnouncementAttachmentBatchService service;
    public AnnouncementAttachmentBatchController(AnnouncementAttachmentBatchService service) {this.service=service;}
    @PostMapping("/scope-preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Preview>> selectScopePreview(Authentication actor,@Valid @RequestBody AttachmentBatchRequests.Scope scope) {
        return response(service.selectScopePreview(actor,scope));
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> insertBatch(Authentication actor,@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AttachmentBatchRequests.Reservation request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertBatch(actor,key,request)));
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentBatchResponses.Batch>>> selectBatchList(Authentication actor,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return response(service.selectBatchList(actor,page,size));}
    @GetMapping("/{batchId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> selectBatchDetails(Authentication actor,@PathVariable UUID batchId) {return response(service.selectBatchDetails(actor,batchId));}
    @GetMapping("/{batchId}/items") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentBatchRows.Item>>> selectItemList(Authentication actor,@PathVariable UUID batchId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return response(service.selectItemList(actor,batchId,page,size));}
    @PutMapping("/{batchId}/scope-cancellation") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> updateScopeCancellation(Authentication actor,@PathVariable UUID batchId,
            @Valid @RequestBody AttachmentBatchRequests.Cancellation request) {return response(service.updateScopeCancellation(actor,batchId,request));}
    private <T> ResponseEntity<ApiResponse<T>> response(T value) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(value));}
    @PutMapping("/{batchId}/collection") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> updateCollectionStart(Authentication actor,@PathVariable UUID batchId,
            @Valid @RequestBody AttachmentBatchRequests.Collection request) {return response(service.updateCollectionStart(actor,batchId,request));}
    @PutMapping("/{batchId}/collection-pause") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> updateCollectionPause(Authentication actor,@PathVariable UUID batchId,
            @Valid @RequestBody AttachmentBatchRequests.Pause request) {return response(service.updateCollectionPause(actor,batchId,request));}
    @PutMapping("/{batchId}/collection-resume") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBatchResponses.Batch>> updateCollectionResume(Authentication actor,@PathVariable UUID batchId,
            @Valid @RequestBody AttachmentBatchRequests.Collection request) {return response(service.updateCollectionResume(actor,batchId,request));}
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "정책·배치 ID와 멱등 키는 UUID, 날짜는 ISO 형식으로 입력하세요. 수집 시작·재개에는 조회한 버전·범위 지문·대상 및 삭제 건수·최대 bytes/HTTP·사유가 필요합니다. URL·파서·대상 ID·성공값은 지정할 수 없습니다."));
    }
}

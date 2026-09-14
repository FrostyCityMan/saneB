package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBackfillService;
import com.saneb.domain.announcementattachment.vo.AttachmentBackfillRows;
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
@RequestMapping("/api/v2/admin/announcement-attachment-backfills")
public class AnnouncementAttachmentBackfillController {
    private final AnnouncementAttachmentBackfillService service;
    public AnnouncementAttachmentBackfillController(AnnouncementAttachmentBackfillService service) {this.service=service;}
    @PostMapping("/scope-preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBackfillResponses.Preview>> selectScopePreview(Authentication actor,@Valid @RequestBody AttachmentBackfillRequests.Scope scope) {
        return response(service.selectScopePreview(actor,scope));
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBackfillResponses.Inventory>> insertInventory(Authentication actor,@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AttachmentBackfillRequests.Inventory request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertInventory(actor,key,request)));
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentBackfillResponses.Inventory>>> selectRunList(Authentication actor,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return response(service.selectRunList(actor,page,size));}
    @GetMapping("/{runId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBackfillResponses.Inventory>> selectRunDetails(Authentication actor,@PathVariable UUID runId) {return response(service.selectRunDetails(actor,runId));}
    @GetMapping("/{runId}/segments") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentBackfillRows.Segment>>> selectSegmentList(Authentication actor,@PathVariable UUID runId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return response(service.selectSegmentList(actor,runId,page,size));}
    @GetMapping("/{runId}/segments/{segmentNo}/items") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentBackfillRows.Item>>> selectItemList(Authentication actor,@PathVariable UUID runId,@PathVariable long segmentNo,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return response(service.selectItemList(actor,runId,segmentNo,page,size));}
    private <T> ResponseEntity<ApiResponse<T>> response(T value) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(value));}
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "정책·전체 목록 ID와 멱등 키는 UUID, 시각은 ISO 형식으로 입력하세요. 전체 목록 고정에는 조회한 범위 지문·전체 후보 수·사유가 필요합니다. 분할 크기는 1~1000이며 전체 건수 제한·URL·파서·대상 ID는 지정할 수 없습니다."));
    }
}

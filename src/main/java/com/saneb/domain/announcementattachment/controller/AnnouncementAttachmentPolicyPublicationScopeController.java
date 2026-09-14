package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationScopeService;
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
@RequestMapping("/api/v2/admin/announcement-attachment-policies/{policyId}/publication-scopes")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
public class AnnouncementAttachmentPolicyPublicationScopeController {
    private final AnnouncementAttachmentPolicyPublicationScopeService service;
    public AnnouncementAttachmentPolicyPublicationScopeController(AnnouncementAttachmentPolicyPublicationScopeService service){this.service=service;}
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Details>> insertScope(Authentication actor,@PathVariable UUID policyId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Prepare request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertScope(actor,policyId,key,request)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Summary>>> selectScopeList(Authentication actor,@PathVariable UUID policyId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectScopeList(actor,policyId,page,size)));
    }
    @GetMapping("/{scopeId}")
    public ResponseEntity<ApiResponse<Details>> selectScopeDetails(Authentication actor,@PathVariable UUID policyId,@PathVariable UUID scopeId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectScopeDetails(actor,policyId,scopeId)));
    }
    @GetMapping("/{scopeId}/items")
    public ResponseEntity<ApiResponse<PageResponse<Item>>> selectItemList(Authentication actor,@PathVariable UUID policyId,@PathVariable UUID scopeId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectItemList(actor,policyId,scopeId,page,size)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception ignored) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "정책·범위·멱등 키는 UUID여야 합니다. 준비 입력에는 조회 버전과 사유만 사용하며 대상 ID 목록·hash·QA 성공·게시 상태는 지정할 수 없습니다."));
    }
}

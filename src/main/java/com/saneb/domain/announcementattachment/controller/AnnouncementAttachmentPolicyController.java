package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-attachment-policies")
public class AnnouncementAttachmentPolicyController {
    private final AnnouncementAttachmentPolicyService service;
    public AnnouncementAttachmentPolicyController(AnnouncementAttachmentPolicyService service) { this.service=service; }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentPolicyResponses.Summary>>> selectPolicyList(Authentication actor,
            @RequestParam(required=false) String status,@RequestParam(required=false) UUID ruleReleaseId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectPolicyList(actor,status,ruleReleaseId,page,size)));
    }
    @GetMapping("/{policyId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentPolicyResponses.Details>> selectPolicyDetails(Authentication actor,@PathVariable UUID policyId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectPolicyDetails(actor,policyId)));
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentPolicyResponses.Details>> insertPolicy(Authentication actor,@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AttachmentPolicyRequests.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertPolicy(actor,key,request)));
    }
    @PutMapping("/{policyId}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentPolicyResponses.Details>> updatePolicyDraft(Authentication actor,@PathVariable UUID policyId,
            @Valid @RequestBody AttachmentPolicyRequests.Update request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.updatePolicyDraft(actor,policyId,request)));
    }
    @PostMapping("/{policyId}/revisions") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentPolicyResponses.Details>> insertPolicyRevision(Authentication actor,@PathVariable UUID policyId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentPolicyRequests.Revision request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertPolicyRevision(actor,policyId,key,request)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifier(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "정책·규칙 식별자와 Idempotency-Key는 UUID여야 하며 page/size는 정수여야 합니다."));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID),
                "정책 입력 형식이 올바르지 않습니다. 규칙·모드·공고별 한도·조회 버전·사유·구간 규칙 버전만 사용하며 URL, parser, 임의 지문·실행 설정·게시 상태는 직접 지정할 수 없습니다."));
    }
}

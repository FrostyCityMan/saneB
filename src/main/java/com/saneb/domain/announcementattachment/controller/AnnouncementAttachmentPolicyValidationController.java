package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyValidationResponse;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyValidationService;
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
@RequestMapping("/api/v2/admin/announcement-attachment-policies/{policyId}/validation-runs")
public class AnnouncementAttachmentPolicyValidationController {
    private final AnnouncementAttachmentPolicyValidationService service;
    public AnnouncementAttachmentPolicyValidationController(AnnouncementAttachmentPolicyValidationService service) {this.service=service;}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentPolicyValidationResponse>>> selectRunList(Authentication actor,@PathVariable UUID policyId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectRunList(actor,policyId,page,size)));
    }
    @GetMapping("/{runId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentPolicyValidationResponse>> selectRunDetails(Authentication actor,@PathVariable UUID policyId,@PathVariable UUID runId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectRunDetails(actor,policyId,runId)));
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentPolicyValidationResponse>> insertRun(Authentication actor,@PathVariable UUID policyId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentPolicyCheckRequest request) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertRun(actor,policyId,key,request)));
    }
    @PutMapping("/{runId}/cancellation") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentPolicyValidationResponse>> updateCancellation(Authentication actor,@PathVariable UUID policyId,@PathVariable UUID runId,
            @Valid @RequestBody AttachmentPolicyCheckRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.updateCancellation(actor,policyId,runId,request)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifiers(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),"정책·실행 ID·Idempotency-Key는 UUID, 페이지 값은 정수여야 합니다."));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID),
                "조회 버전과 사유만 입력하세요. QA 성공값·파일 경로·대상·규칙·실행 설정을 지정할 수 없습니다."));
    }
}

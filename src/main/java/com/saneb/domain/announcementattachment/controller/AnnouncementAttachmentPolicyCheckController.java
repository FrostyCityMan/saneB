package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyCheckService;
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
@RequestMapping("/api/v2/admin/announcement-attachment-policies/{policyId}/classification-checks")
public class AnnouncementAttachmentPolicyCheckController {
    private final AnnouncementAttachmentPolicyCheckService service;
    public AnnouncementAttachmentPolicyCheckController(AnnouncementAttachmentPolicyCheckService service) {this.service=service;}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentPolicyCheckResponse>>> selectCheckList(Authentication actor,@PathVariable UUID policyId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectCheckList(actor,policyId,page,size)));
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentPolicyCheckResponse>> insertClassificationCheck(Authentication actor,@PathVariable UUID policyId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentPolicyCheckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertClassificationCheck(actor,policyId,key,request)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifiers(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),"정책 ID·Idempotency-Key는 UUID여야 하며 page/size는 정수여야 합니다."));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.ANNOUNCEMENT_ATTACHMENT_POLICY_INVALID),"조회 버전과 사유만 입력하세요. QA 성공값·규칙·파서·실행 설정을 요청으로 지정할 수 없습니다."));
    }
}

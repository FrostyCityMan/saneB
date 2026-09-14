package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaPlanResponse;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyValidationService;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-attachment-policies/{policyId}/provider-qa-plan")
public class AnnouncementAttachmentProviderQaPlanController {
    private final AnnouncementAttachmentPolicyValidationService service;
    public AnnouncementAttachmentProviderQaPlanController(AnnouncementAttachmentPolicyValidationService service) {this.service=service;}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentProviderQaPlanResponse>> selectProviderQaPlan(Authentication actor,@PathVariable UUID policyId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectProviderQaPlan(actor,policyId,page,size)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifier() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "정책 ID는 UUID 형식이며 페이지와 페이지 크기는 정수여야 합니다."));
    }
}

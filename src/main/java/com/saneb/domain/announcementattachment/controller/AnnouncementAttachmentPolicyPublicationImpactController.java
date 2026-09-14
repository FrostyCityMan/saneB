package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationImpact;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationImpactService;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-attachment-policies/{policyId}/publication-impact")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
public class AnnouncementAttachmentPolicyPublicationImpactController {
    private final AnnouncementAttachmentPolicyPublicationImpactService service;
    public AnnouncementAttachmentPolicyPublicationImpactController(AnnouncementAttachmentPolicyPublicationImpactService service){this.service=service;}
    @GetMapping public ResponseEntity<ApiResponse<AttachmentPolicyPublicationImpact>> selectImpactDetails(Authentication actor,@PathVariable UUID policyId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectImpactDetails(actor,policyId)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception ignored) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),"정책 ID는 UUID 형식이어야 합니다. 정책 목록에서 대상을 다시 선택하세요."));
    }
}

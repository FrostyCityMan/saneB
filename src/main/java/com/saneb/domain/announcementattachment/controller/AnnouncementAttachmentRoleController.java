package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.ApiResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentRoleRequest;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentRoleService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}")
public class AnnouncementAttachmentRoleController {
    private final AnnouncementAttachmentRoleService service;
    public AnnouncementAttachmentRoleController(AnnouncementAttachmentRoleService service) { this.service=service; }
    @PutMapping("/attachment-roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AttachmentJobResponse>> insertRoleChange(Authentication authentication,@PathVariable UUID sourceId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentRoleRequest request) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertRoleChange(authentication,sourceId,key,request)));
    }
    @GetMapping("/attachment-jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentJobResponse>> selectJobDetails(@PathVariable UUID sourceId,@PathVariable UUID jobId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectJobDetails(sourceId,jobId)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIdentifiers(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                exception instanceof MissingRequestHeaderException ? "역할 변경에는 UUID 형식의 Idempotency-Key 헤더가 필요합니다."
                        : "원문·작업 식별자와 Idempotency-Key는 UUID 형식이어야 합니다."));
    }
}

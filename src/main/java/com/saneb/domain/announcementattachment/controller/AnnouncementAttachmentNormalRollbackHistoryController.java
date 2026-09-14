package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackResponses.JobSummary;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentNormalRollbackService;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/attachment-recovery-jobs")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
public class AnnouncementAttachmentNormalRollbackHistoryController {
    private final AnnouncementAttachmentNormalRollbackService service;
    public AnnouncementAttachmentNormalRollbackHistoryController(AnnouncementAttachmentNormalRollbackService service) { this.service=service; }
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobSummary>>> selectJobList(Authentication actor,@PathVariable UUID sourceId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectJobList(actor,sourceId,page,size)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(MethodArgumentTypeMismatchException ignored) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "원문 식별자는 UUID, 페이지(page)는 1~1000000, 페이지 크기(size)는 1~100의 정수로 입력하세요."));
    }
}

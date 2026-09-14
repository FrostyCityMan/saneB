package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.History;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchHistoryService;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-attachment-batches/{batchId}/action-history")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
public class AnnouncementAttachmentBatchHistoryController {
    private final AnnouncementAttachmentBatchHistoryService service;
    public AnnouncementAttachmentBatchHistoryController(AnnouncementAttachmentBatchHistoryService service){this.service=service;}
    @GetMapping
    public ResponseEntity<ApiResponse<History>> selectActionList(Authentication actor,@PathVariable UUID batchId,
            @RequestParam(required=false) Integer throughVersion,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectActionList(actor,batchId,throughVersion,page,size)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception ignored){return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
            "배치 ID는 UUID, 페이지(page)는 1 이상, 크기(size)는 1~100, 조회 기준 버전(throughVersion)은 0 이상의 정수로 입력하세요."));}
}

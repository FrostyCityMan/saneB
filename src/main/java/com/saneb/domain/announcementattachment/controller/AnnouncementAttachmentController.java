package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentEvidenceResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentReadService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'APPROVER')")
public class AnnouncementAttachmentController {
    private final AnnouncementAttachmentReadService service;
    public AnnouncementAttachmentController(AnnouncementAttachmentReadService service) { this.service = service; }

    @GetMapping("/attachment-sets")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentEvidenceResponses.SetSummary>>> selectAttachmentSetList(
            @PathVariable UUID sourceId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectAttachmentSetList(sourceId, page, size)));
    }
    @GetMapping("/attachment-sets/{setId}/files")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentEvidenceResponses.FileSummary>>> selectAttachmentFileList(
            @PathVariable UUID sourceId, @PathVariable UUID setId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectAttachmentFileList(sourceId, setId, page, size)));
    }
    @GetMapping("/attachment-extractions/{extractionId}/blocks")
    public ResponseEntity<ApiResponse<PageResponse<AttachmentEvidenceResponses.Block>>> selectAttachmentBlockList(
            @PathVariable UUID sourceId, @PathVariable UUID extractionId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int textOffset, @RequestParam(defaultValue = "2000") int textLimit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(
                service.selectAttachmentBlockList(sourceId, extractionId, page, size, textOffset, textLimit)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleParameterTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = UUID.class.equals(exception.getRequiredType())
                ? "원문·첨부 근거 식별자는 UUID 형식이어야 합니다. 목록에서 대상을 다시 선택해 주세요."
                : "페이지·본문 위치·조회 길이는 정수로 입력해야 합니다.";
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED), message));
    }
}

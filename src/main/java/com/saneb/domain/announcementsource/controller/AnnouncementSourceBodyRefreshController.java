package com.saneb.domain.announcementsource.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.error.ErrorResponse;
import com.saneb.domain.announcementsource.dto.SourceBodyRefreshResponses;
import com.saneb.domain.announcementsource.service.AnnouncementSourceBodyRefreshService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v2/admin/announcement-sources/{sourceId}/body-refresh-previews")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AnnouncementSourceBodyRefreshController {
    private final AnnouncementSourceBodyRefreshService service;
    public AnnouncementSourceBodyRefreshController(AnnouncementSourceBodyRefreshService service) { this.service=service; }
    @PostMapping
    public ResponseEntity<ApiResponse<SourceBodyRefreshResponses.Preview>> insertPreview(Authentication auth,@PathVariable UUID sourceId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertPreview(auth,sourceId)));
    }
    @PostMapping("/{previewId}/apply")
    public ResponseEntity<ApiResponse<SourceBodyRefreshResponses.Applied>> savePreview(Authentication auth,@PathVariable UUID sourceId,@PathVariable UUID previewId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.savePreview(auth,sourceId,previewId)));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleParameterTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(
                ErrorResponse.of(ErrorCode.VALIDATION_FAILED),"공고·본문 미리보기 식별자는 UUID 형식이어야 합니다. 목록에서 공고를 다시 선택하세요."));
    }
}

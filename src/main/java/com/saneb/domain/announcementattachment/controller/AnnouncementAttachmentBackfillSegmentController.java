package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBackfillSegmentService;
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
@RequestMapping("/api/v2/admin/announcement-attachment-backfills")
public class AnnouncementAttachmentBackfillSegmentController {
    private final AnnouncementAttachmentBackfillSegmentService service;
    public AnnouncementAttachmentBackfillSegmentController(AnnouncementAttachmentBackfillSegmentService service) {this.service=service;}
    @GetMapping("/{runId}/segments/{segmentNo}/reservation-preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBackfillSegmentResponses.Preview>> selectReservationPreview(Authentication actor,@PathVariable UUID runId,@PathVariable long segmentNo) {
        return response(service.selectReservationPreview(actor,runId,segmentNo));
    }
    @PostMapping("/{runId}/segments/{segmentNo}/reservation") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentBackfillSegmentResponses.Reservation>> insertReservation(Authentication actor,@PathVariable UUID runId,@PathVariable long segmentNo,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttachmentBackfillSegmentRequests.Reservation request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertReservation(actor,runId,segmentNo,key,request)));
    }
    @GetMapping("/{runId}/summary") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
    public ResponseEntity<ApiResponse<AttachmentBackfillSegmentResponses.Summary>> selectSummaryDetails(Authentication actor,@PathVariable UUID runId) {return response(service.selectSummaryDetails(actor,runId));}
    private <T> ResponseEntity<ApiResponse<T>> response(T value) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(value));}
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception exception) {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),
                "전체 목록 ID·멱등 키는 UUID, 분할 번호는 양수로 입력하세요. 예약에는 조회한 목록 버전·분할 지문·잔여/삭제 건수·사유가 필요합니다. 대상 ID·URL·파서·실행 결과는 지정할 수 없습니다."));
    }
}

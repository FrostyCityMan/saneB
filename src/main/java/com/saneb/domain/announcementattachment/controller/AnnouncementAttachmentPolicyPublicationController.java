package com.saneb.domain.announcementattachment.controller;

import com.saneb.common.error.*;
import com.saneb.common.response.*;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublication.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentPolicyPublicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController @RequestMapping("/api/v2/admin/announcement-attachment-policies/{policyId}/publication")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','APPROVER')")
public class AnnouncementAttachmentPolicyPublicationController {
    private final AnnouncementAttachmentPolicyPublicationService service;
    public AnnouncementAttachmentPolicyPublicationController(AnnouncementAttachmentPolicyPublicationService service){this.service=service;}
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Result>> insertPublication(Authentication actor,@PathVariable UUID policyId,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Request request){
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.insertPublication(actor,policyId,key,request)));
    }
    @GetMapping public ResponseEntity<ApiResponse<Result>> selectPublicationDetails(Authentication actor,@PathVariable UUID policyId){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.selectPublicationDetails(actor,policyId)));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInput(Exception ignored){return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
            .body(ApiResponse.failure(ErrorResponse.of(ErrorCode.VALIDATION_FAILED),"게시에는 UUID 정책·준비 범위·멱등 키와 준비 지문·조회 버전·영향 확인 세 항목·사유가 필요합니다. QA 결과나 임의 정책 설정은 제출할 수 없습니다."));}
}

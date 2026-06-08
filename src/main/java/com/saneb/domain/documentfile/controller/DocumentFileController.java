package com.saneb.domain.documentfile.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.documentfile.dto.DocumentSubmissionCreateRequest;
import com.saneb.domain.documentfile.dto.DocumentSubmissionResponse;
import com.saneb.domain.documentfile.dto.DocumentSubmissionReviewRequest;
import com.saneb.domain.documentfile.dto.StoredFileResponse;
import com.saneb.domain.documentfile.service.DocumentFileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1")
public class DocumentFileController {

    private final DocumentFileService documentFileService;

    public DocumentFileController(DocumentFileService documentFileService) {
        this.documentFileService = documentFileService;
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StoredFileResponse> insertStoredFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(documentFileService.insertStoredFile(authentication, file));
    }

    @GetMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StoredFileResponse> selectStoredFileDetails(
            Authentication authentication,
            @PathVariable UUID fileId
    ) {
        return ApiResponse.success(documentFileService.selectStoredFileDetails(authentication, fileId));
    }

    @PostMapping("/document-submissions")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public ApiResponse<DocumentSubmissionResponse> insertDocumentSubmission(
            Authentication authentication,
            @Valid @RequestBody DocumentSubmissionCreateRequest request
    ) {
        return ApiResponse.success(documentFileService.insertDocumentSubmission(authentication, request));
    }

    @GetMapping("/document-submissions")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<PageResponse<DocumentSubmissionResponse>> selectDocumentSubmissionList(
            Authentication authentication,
            @RequestParam(required = false) String resourceTypeCode,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(documentFileService.selectDocumentSubmissionList(
                authentication,
                resourceTypeCode,
                resourceId,
                statusCode,
                page,
                size
        ));
    }

    @PatchMapping("/document-submissions/{submissionId}/review")
    @PreAuthorize("hasAnyRole('PARTNER', 'OPERATOR', 'APPROVER', 'ADMIN')")
    public ApiResponse<DocumentSubmissionResponse> updateDocumentSubmissionReview(
            Authentication authentication,
            @PathVariable UUID submissionId,
            @Valid @RequestBody DocumentSubmissionReviewRequest request
    ) {
        return ApiResponse.success(documentFileService.updateDocumentSubmissionReview(
                authentication,
                submissionId,
                request
        ));
    }
}

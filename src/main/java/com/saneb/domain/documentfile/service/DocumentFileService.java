package com.saneb.domain.documentfile.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.documentfile.dto.DocumentSubmissionCreateRequest;
import com.saneb.domain.documentfile.dto.DocumentSubmissionResponse;
import com.saneb.domain.documentfile.dto.DocumentSubmissionReviewRequest;
import com.saneb.domain.documentfile.dto.StoredFileResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentFileService {

    StoredFileResponse insertStoredFile(Authentication authentication, MultipartFile file);

    StoredFileResponse selectStoredFileDetails(Authentication authentication, UUID fileId);

    DocumentSubmissionResponse insertDocumentSubmission(
            Authentication authentication,
            DocumentSubmissionCreateRequest request
    );

    PageResponse<DocumentSubmissionResponse> selectDocumentSubmissionList(
            Authentication authentication,
            String resourceTypeCode,
            UUID resourceId,
            String statusCode,
            int page,
            int size
    );

    DocumentSubmissionResponse updateDocumentSubmissionReview(
            Authentication authentication,
            UUID submissionId,
            DocumentSubmissionReviewRequest request
    );
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DocumentFileService.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param file 입력 값
     *
     * @return 처리 결과
     */
    StoredFileResponse insertStoredFile(Authentication authentication, MultipartFile file);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param fileId 입력 값
     *
     * @return 처리 결과
     */
    StoredFileResponse selectStoredFileDetails(Authentication authentication, UUID fileId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    DocumentSubmissionResponse insertDocumentSubmission(
            Authentication authentication,
            DocumentSubmissionCreateRequest request
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param resourceTypeCode 입력 값
     *
     * @param resourceId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<DocumentSubmissionResponse> selectDocumentSubmissionList(
            Authentication authentication,
            String resourceTypeCode,
            UUID resourceId,
            String statusCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param submissionId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    DocumentSubmissionResponse updateDocumentSubmissionReview(
            Authentication authentication,
            UUID submissionId,
            DocumentSubmissionReviewRequest request
    );
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DocumentFileServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.documentfile.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.documentfile.dao.DocumentFileDao;
import com.saneb.domain.documentfile.dto.DocumentSubmissionCreateRequest;
import com.saneb.domain.documentfile.dto.DocumentSubmissionResponse;
import com.saneb.domain.documentfile.dto.DocumentSubmissionReviewRequest;
import com.saneb.domain.documentfile.dto.StoredFileResponse;
import com.saneb.domain.documentfile.service.DocumentFileService;
import com.saneb.domain.documentfile.vo.ApplicationProgressAccessRow;
import com.saneb.domain.documentfile.vo.AuditLogCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionInsertCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionReviewCommand;
import com.saneb.domain.documentfile.vo.DocumentSubmissionRow;
import com.saneb.domain.documentfile.vo.DocumentSubmissionSearchCondition;
import com.saneb.domain.documentfile.vo.PartnerVerificationAccessRow;
import com.saneb.domain.documentfile.vo.StoredFileInsertCommand;
import com.saneb.domain.documentfile.vo.StoredFileRow;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentFileServiceImpl implements DocumentFileService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 120;
    private static final Set<String> OPERATING_ROLES = Set.of("PARTNER", "OPERATOR", "APPROVER", "ADMIN");
    private static final Set<String> SUBMISSION_RESOURCE_TYPES = Set.of(
            "PARTNER_VERIFICATION",
            "APPLICATION_PROGRESS"
    );
    private static final Set<String> SUBMISSION_STATUS_CODES = Set.of("SUBMITTED", "APPROVED", "REJECTED");
    private static final Set<String> REVIEW_STATUS_CODES = Set.of("APPROVED", "REJECTED");

    private final DocumentFileDao documentFileDao;
    private final Path storageRoot;
    private final long maxUploadBytes;

    /**
     * 객체를 생성합니다.
     *
     * @param documentFileDao 입력 값
     *
     * @param environment 입력 값
     */
    @Autowired
    public DocumentFileServiceImpl(
            DocumentFileDao documentFileDao,
            Environment environment
    ) {
        this(
                documentFileDao,
                environment.getProperty("saneb.storage.root", "build/saneb-storage"),
                environment.getProperty("saneb.storage.max-upload-bytes", Long.class, 10485760L)
        );
    }

    /**
     * 객체를 생성합니다.
     *
     * @param documentFileDao 입력 값
     *
     * @param storageRoot 입력 값
     *
     * @param maxUploadBytes 입력 값
     */
    DocumentFileServiceImpl(
            DocumentFileDao documentFileDao,
            String storageRoot,
            long maxUploadBytes
    ) {
        this.documentFileDao = documentFileDao;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.maxUploadBytes = maxUploadBytes;
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param file 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param file 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public StoredFileResponse insertStoredFile(Authentication authentication, MultipartFile file) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        validateUploadFile(file);

        UUID fileId = UUID.randomUUID();
        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        String storedFilename = fileId + selectSafeExtension(originalFilename);
        String storageKey = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/")) + storedFilename;
        Path targetPath = resolveStoragePath(storageKey);
        byte[] fileBytes = readFileBytes(file);
        String checksumSha256 = checksumSha256(fileBytes);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, fileBytes);
            documentFileDao.insertStoredFile(new StoredFileInsertCommand(
                    fileId,
                    actor.userId(),
                    originalFilename,
                    storedFilename,
                    storageKey,
                    normalizeContentType(file.getContentType()),
                    file.getSize(),
                    checksumSha256,
                    actor.userId()
            ));
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(targetPath);
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 저장 중 오류가 발생했습니다."
            );
        }

        return toStoredFileResponse(selectStoredFileRow(fileId));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param fileId 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public StoredFileResponse selectStoredFileDetails(Authentication authentication, UUID fileId) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        StoredFileRow row = selectStoredFileRow(fileId);
        validateFileAccess(actor, row);
        return toStoredFileResponse(row);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public DocumentSubmissionResponse insertDocumentSubmission(
            Authentication authentication,
            DocumentSubmissionCreateRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        StoredFileRow file = selectStoredFileRow(request.fileId());
        validateFileAccess(actor, file);

        String resourceTypeCode = normalizeRequiredCode(
                "resourceTypeCode",
                request.resourceTypeCode(),
                SUBMISSION_RESOURCE_TYPES
        );
        validateResourceAccess(actor, resourceTypeCode, request.resourceId());
        String documentTypeCode = normalizeDocumentTypeCode(request.documentTypeCode());

        UUID submissionId = UUID.randomUUID();
        documentFileDao.insertDocumentSubmission(new DocumentSubmissionInsertCommand(
                submissionId,
                file.fileId(),
                actor.userId(),
                resourceTypeCode,
                request.resourceId(),
                documentTypeCode
        ));
        insertAudit(actor.userId(), "DOCUMENT_SUBMISSION_CREATE", submissionId, metadata(
                "fileId", file.fileId().toString(),
                "resourceTypeCode", resourceTypeCode,
                "documentTypeCode", documentTypeCode
        ));
        return toDocumentSubmissionResponse(selectDocumentSubmissionRow(submissionId));
    }

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
    @Override
    public PageResponse<DocumentSubmissionResponse> selectDocumentSubmissionList(
            Authentication authentication,
            String resourceTypeCode,
            UUID resourceId,
            String statusCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String normalizedResourceTypeCode = normalizeOptionalCode(resourceTypeCode);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        validateOptionalCode("resourceTypeCode", normalizedResourceTypeCode, SUBMISSION_RESOURCE_TYPES);
        validateOptionalCode("statusCode", normalizedStatusCode, SUBMISSION_STATUS_CODES);

        DocumentSubmissionSearchCondition condition = new DocumentSubmissionSearchCondition(
                hasOperatingRole(actor) ? null : actor.userId(),
                normalizedResourceTypeCode,
                resourceId,
                normalizedStatusCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = documentFileDao.selectDocumentSubmissionCount(condition);
        List<DocumentSubmissionResponse> items = documentFileDao.selectDocumentSubmissionList(condition).stream()
                .map(this::toDocumentSubmissionResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

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
    @Override
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
    @Transactional
    public DocumentSubmissionResponse updateDocumentSubmissionReview(
            Authentication authentication,
            UUID submissionId,
            DocumentSubmissionReviewRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredPrincipal(authentication);
        String afterStatusCode = normalizeRequiredCode("statusCode", request.statusCode(), REVIEW_STATUS_CODES);
        DocumentSubmissionRow row = selectDocumentSubmissionRow(submissionId);

        DocumentSubmissionReviewCommand command = new DocumentSubmissionReviewCommand(
                UUID.randomUUID(),
                submissionId,
                actor.userId(),
                row.statusCode(),
                afterStatusCode,
                trimToNull(request.reviewNote())
        );
        int updatedCount = documentFileDao.updateDocumentSubmissionReview(command);
        if (updatedCount == 0) {
            throw notFound("서류 제출 내역을 찾을 수 없습니다.");
        }
        documentFileDao.insertDocumentSubmissionReview(command);
        insertAudit(actor.userId(), "DOCUMENT_SUBMISSION_REVIEW", submissionId, metadata(
                "beforeStatusCode", row.statusCode(),
                "afterStatusCode", afterStatusCode,
                "reviewNoteProvided", String.valueOf(trimToNull(request.reviewNote()) != null)
        ));
        return toDocumentSubmissionResponse(selectDocumentSubmissionRow(submissionId));
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param file 입력 값
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw validationFailed("업로드할 파일을 선택하세요.");
        }
        if (file.getSize() <= 0) {
            throw validationFailed("비어 있는 파일은 업로드할 수 없습니다.");
        }
        if (file.getSize() > maxUploadBytes) {
            throw validationFailed("10MB 이하 파일만 업로드할 수 있습니다.");
        }
    }

    /**
     * 입력 데이터를 읽어 처리합니다.
     *
     * @param file 입력 값
     *
     * @return 처리 결과
     */
    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일을 읽는 중 오류가 발생했습니다."
            );
        }
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param fileBytes 입력 값
     *
     * @return 처리 결과
     */
    private String checksumSha256(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(fileBytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 확인값 생성 중 오류가 발생했습니다."
            );
        }
    }

    /**
     * 업무 처리에 필요한 값을 해석합니다.
     *
     * @param storageKey 입력 값
     *
     * @return 처리 결과
     */
    private Path resolveStoragePath(String storageKey) {
        Path targetPath = storageRoot.resolve(storageKey).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw validationFailed("파일 저장 경로가 올바르지 않습니다.");
        }
        return targetPath;
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param filename 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeOriginalFilename(String filename) {
        String cleaned = StringUtils.cleanPath(filename == null ? "upload.bin" : filename);
        String nameOnly = Path.of(cleaned).getFileName().toString();
        if (nameOnly.isBlank() || nameOnly.contains("..")) {
            throw validationFailed("파일명이 올바르지 않습니다.");
        }
        return nameOnly.length() > MAX_FILENAME_LENGTH ? nameOnly.substring(0, MAX_FILENAME_LENGTH) : nameOnly;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param originalFilename 입력 값
     *
     * @return 처리 결과
     */
    private String selectSafeExtension(String originalFilename) {
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (extension.length() > 20 || !extension.matches("\\.[a-z0-9]+")) {
            return "";
        }
        return extension;
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param contentType 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeContentType(String contentType) {
        String value = trimToNull(contentType);
        if (value == null) {
            return null;
        }
        return value.length() > MAX_CONTENT_TYPE_LENGTH ? value.substring(0, MAX_CONTENT_TYPE_LENGTH) : value;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param actor 입력 값
     *
     * @param file 입력 값
     */
    private void validateFileAccess(AuthenticatedUserDetails actor, StoredFileRow file) {
        if (!file.ownerUserId().equals(actor.userId()) && !hasOperatingRole(actor)) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "파일에 접근할 수 없습니다.");
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param actor 입력 값
     *
     * @param resourceTypeCode 입력 값
     *
     * @param resourceId 입력 값
     */
    private void validateResourceAccess(AuthenticatedUserDetails actor, String resourceTypeCode, UUID resourceId) {
        if ("PARTNER_VERIFICATION".equals(resourceTypeCode)) {
            PartnerVerificationAccessRow row = documentFileDao.selectPartnerVerificationAccess(resourceId);
            if (row == null) {
                throw notFound("검증 건을 찾을 수 없습니다.");
            }
            if (!hasOperatingRole(actor) && !row.memberUserId().equals(actor.userId())) {
                throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "검증 건에 접근할 수 없습니다.");
            }
            return;
        }
        ApplicationProgressAccessRow row = documentFileDao.selectApplicationProgressAccess(resourceId);
        if (row == null) {
            throw notFound("신청 진행 건을 찾을 수 없습니다.");
        }
        if (!hasOperatingRole(actor) && !row.memberUserId().equals(actor.userId())) {
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "신청 진행 건에 접근할 수 없습니다.");
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param fileId 입력 값
     *
     * @return 처리 결과
     */
    private StoredFileRow selectStoredFileRow(UUID fileId) {
        StoredFileRow row = documentFileDao.selectStoredFileDetails(fileId);
        if (row == null) {
            throw notFound("파일을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param submissionId 입력 값
     *
     * @return 처리 결과
     */
    private DocumentSubmissionRow selectDocumentSubmissionRow(UUID submissionId) {
        DocumentSubmissionRow row = documentFileDao.selectDocumentSubmissionDetails(submissionId);
        if (row == null) {
            throw notFound("서류 제출 내역을 찾을 수 없습니다.");
        }
        return row;
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private StoredFileResponse toStoredFileResponse(StoredFileRow row) {
        return new StoredFileResponse(
                row.fileId(),
                row.ownerUserId(),
                row.originalFilename(),
                row.contentType(),
                row.fileSize(),
                row.checksumSha256(),
                row.statusCode(),
                row.createdAt()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private DocumentSubmissionResponse toDocumentSubmissionResponse(DocumentSubmissionRow row) {
        return new DocumentSubmissionResponse(
                row.submissionId(),
                row.fileId(),
                row.originalFilename(),
                row.contentType(),
                row.fileSize(),
                row.resourceTypeCode(),
                row.resourceId(),
                row.documentTypeCode(),
                row.statusCode(),
                row.submittedBy(),
                row.submittedAt(),
                row.reviewedBy(),
                row.reviewedAt(),
                row.reviewNote()
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails selectRequiredPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal;
        }
        throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "DB 인증 사용자만 사용할 수 있습니다.");
    }

    /**
     * 조건 충족 여부를 확인합니다.
     *
     * @param actor 입력 값
     *
     * @return 처리 결과
     */
    private boolean hasOperatingRole(AuthenticatedUserDetails actor) {
        return actor.roles().stream().anyMatch(OPERATING_ROLES::contains);
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     */
    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.INVALID_PAGE_REQUEST, HttpStatus.BAD_REQUEST, "페이지 요청 값이 올바르지 않습니다.");
        }
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeDocumentTypeCode(String value) {
        String code = normalizeRequiredCode("documentTypeCode", value, null);
        if (code.length() > 80) {
            throw validationFailed("documentTypeCode is invalid.");
        }
        return code;
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param fieldName 입력 값
     *
     * @param value 입력 값
     *
     * @param allowedValues 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String code = normalizeOptionalCode(value);
        if (code == null || (allowedValues != null && !allowedValues.contains(code))) {
            throw validationFailed(fieldName + " is invalid.");
        }
        return code;
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param fieldName 입력 값
     *
     * @param value 입력 값
     *
     * @param allowedValues 입력 값
     */
    private void validateOptionalCode(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw validationFailed(fieldName + " is invalid.");
        }
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeOptionalCode(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 문자열 입력 값을 정리합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param actionCode 입력 값
     *
     * @param resourceId 입력 값
     *
     * @param metadataJson 입력 값
     */
    private void insertAudit(UUID actorUserId, String actionCode, UUID resourceId, String metadataJson) {
        documentFileDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                "DOCUMENT_SUBMISSION",
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param key1 입력 값
     *
     * @param value1 입력 값
     *
     * @param key2 입력 값
     *
     * @param value2 입력 값
     *
     * @param key3 입력 값
     *
     * @param value3 입력 값
     *
     * @return 처리 결과
     */
    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param targetPath 입력 값
     */
    private void deleteQuietly(Path targetPath) {
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
            // Best-effort cleanup after a failed DB save.
        }
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException validationFailed(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    private ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}

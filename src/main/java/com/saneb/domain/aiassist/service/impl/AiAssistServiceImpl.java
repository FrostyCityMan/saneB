package com.saneb.domain.aiassist.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.aiassist.dao.AiAssistDao;
import com.saneb.domain.aiassist.dto.AiAssistCreateRequest;
import com.saneb.domain.aiassist.dto.AiAssistResponse;
import com.saneb.domain.aiassist.dto.AiAssistReviewRequest;
import com.saneb.domain.aiassist.provider.AiAssistProvider;
import com.saneb.domain.aiassist.provider.AiAssistProviderRequest;
import com.saneb.domain.aiassist.provider.AiAssistProviderResponse;
import com.saneb.domain.aiassist.service.AiAssistService;
import com.saneb.domain.aiassist.vo.AiAssistInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistResultInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistRow;
import com.saneb.domain.aiassist.vo.AiAssistSearchCondition;
import com.saneb.domain.aiassist.vo.AuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAssistServiceImpl implements AiAssistService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ASSIST_TYPE_CODES = Set.of(
            "ANNOUNCEMENT_SUMMARY",
            "DOCUMENT_DRAFT",
            "OPERATION_MEMO_SUMMARY",
            "USER_REPLY_DRAFT"
    );
    private static final Set<String> RESOURCE_TYPES = Set.of(
            "GENERAL",
            "ANNOUNCEMENT",
            "APPLICATION_PROGRESS",
            "MATCHING_CASE",
            "OPERATION_TASK",
            "USER"
    );
    private static final Set<String> REVIEW_STATUS_CODES = Set.of("PENDING_REVIEW", "ACCEPTED", "DISCARDED");

    private final AiAssistDao aiAssistDao;
    private final AiAssistProvider aiAssistProvider;

    public AiAssistServiceImpl(AiAssistDao aiAssistDao, AiAssistProvider aiAssistProvider) {
        this.aiAssistDao = aiAssistDao;
        this.aiAssistProvider = aiAssistProvider;
    }

    @Override
    @Transactional
    public AiAssistResponse insertAiAssistRequest(Authentication authentication, AiAssistCreateRequest request) {
        AuthenticatedUserDetails actor = selectRequiredOperatorPrincipal(authentication);
        String assistTypeCode = normalizeRequiredCode("assistTypeCode", request.assistTypeCode(), ASSIST_TYPE_CODES);
        String resourceType = normalizeOptionalCode(request.resourceType(), "GENERAL", RESOURCE_TYPES);
        String inputText = request.inputText() == null ? "" : request.inputText();
        UUID requestId = UUID.randomUUID();
        long startedAt = System.nanoTime();
        AiAssistProviderResponse providerResponse = aiAssistProvider.generate(new AiAssistProviderRequest(
                assistTypeCode,
                resourceType,
                inputText.length(),
                request.operatorNote() != null && !request.operatorNote().isBlank()
        ));
        int latencyMs = (int) Math.max(0, (System.nanoTime() - startedAt) / 1_000_000L);
        UUID resultId = UUID.randomUUID();

        aiAssistDao.insertAiAssistRequest(new AiAssistInsertCommand(
                requestId,
                assistTypeCode,
                resourceType,
                request.resourceId(),
                sha256(inputText),
                inputText.length(),
                actor.userId(),
                "COMPLETED",
                providerResponse.providerCode(),
                providerResponse.modelCode()
        ));
        aiAssistDao.insertAiAssistResult(new AiAssistResultInsertCommand(
                resultId,
                requestId,
                providerResponse.resultText(),
                "PENDING_REVIEW",
                providerResponse.promptTokenCount(),
                providerResponse.completionTokenCount(),
                latencyMs,
                providerResponse.metadataJson()
        ));
        insertAudit(actor.userId(), "AI_ASSIST_REQUEST_CREATE", requestId, metadata(
                "assistTypeCode", assistTypeCode,
                "resourceType", resourceType,
                "providerCode", providerResponse.providerCode(),
                "modelCode", providerResponse.modelCode(),
                "inputLength", String.valueOf(inputText.length())
        ));
        return toResponse(selectRequiredRequestDetails(requestId));
    }

    @Override
    public PageResponse<AiAssistResponse> selectAiAssistRequestList(
            Authentication authentication,
            String assistTypeCode,
            String resourceType,
            String reviewStatusCode,
            int page,
            int size
    ) {
        selectRequiredOperatorPrincipal(authentication);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        AiAssistSearchCondition condition = new AiAssistSearchCondition(
                normalizeOptionalCode(assistTypeCode, null, ASSIST_TYPE_CODES),
                normalizeOptionalCode(resourceType, null, RESOURCE_TYPES),
                normalizeOptionalCode(reviewStatusCode, null, REVIEW_STATUS_CODES),
                safeSize,
                (safePage - 1) * safeSize
        );
        long totalCount = aiAssistDao.selectAiAssistCount(condition);
        return PageResponse.of(
                aiAssistDao.selectAiAssistList(condition).stream()
                        .map(this::toResponse)
                        .toList(),
                safePage,
                safeSize,
                totalCount
        );
    }

    @Override
    public AiAssistResponse selectAiAssistRequestDetails(Authentication authentication, UUID requestId) {
        selectRequiredOperatorPrincipal(authentication);
        return toResponse(selectRequiredRequestDetails(requestId));
    }

    @Override
    @Transactional
    public AiAssistResponse updateAiAssistResultReview(
            Authentication authentication,
            UUID resultId,
            AiAssistReviewRequest request
    ) {
        AuthenticatedUserDetails actor = selectRequiredOperatorPrincipal(authentication);
        AiAssistRow before = aiAssistDao.selectAiAssistDetailsByResultId(resultId);
        if (before == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "AI 보조 결과를 찾을 수 없습니다.");
        }
        String reviewStatusCode = normalizeRequiredCode("reviewStatusCode", request.reviewStatusCode(), REVIEW_STATUS_CODES);
        aiAssistDao.updateAiAssistResultReviewStatus(resultId, reviewStatusCode, actor.userId());
        insertAudit(actor.userId(), "AI_ASSIST_RESULT_REVIEW", before.requestId(), metadata(
                "reviewStatusCode", reviewStatusCode,
                "assistTypeCode", before.assistTypeCode(),
                "providerCode", before.providerCode(),
                "modelCode", before.modelCode(),
                "inputLength", ""
        ));
        return toResponse(selectRequiredRequestDetails(before.requestId()));
    }

    private AiAssistRow selectRequiredRequestDetails(UUID requestId) {
        AiAssistRow row = aiAssistDao.selectAiAssistDetails(requestId);
        if (row == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "AI 보조 요청을 찾을 수 없습니다.");
        }
        return row;
    }

    private AuthenticatedUserDetails selectRequiredOperatorPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal
                && (principal.roles().contains("OPERATOR") || principal.roles().contains("ADMIN"))) {
            return principal;
        }
        throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "운영 권한이 필요합니다.");
    }

    private String normalizeRequiredCode(String fieldName, String value, Set<String> allowedValues) {
        String code = normalizeOptionalCode(value, null, allowedValues);
        if (code == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, fieldName + " 값이 올바르지 않습니다.");
        }
        return code;
    }

    private String normalizeOptionalCode(String value, String defaultValue, Set<String> allowedValues) {
        String code = value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
        if (code != null && !allowedValues.contains(code)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "코드 값이 올바르지 않습니다.");
        }
        return code;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "해시 처리에 실패했습니다.");
        }
    }

    private AiAssistResponse toResponse(AiAssistRow row) {
        return new AiAssistResponse(
                row.requestId(),
                row.resultId(),
                row.assistTypeCode(),
                row.resourceType(),
                row.resourceId(),
                row.requestStatusCode(),
                row.providerCode(),
                row.modelCode(),
                row.reviewStatusCode(),
                row.resultText(),
                row.requestedBy(),
                row.createdAt(),
                row.completedAt()
        );
    }

    private void insertAudit(UUID actorUserId, String actionCode, UUID requestId, String metadataJson) {
        aiAssistDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                "AI_ASSIST_REQUEST",
                requestId,
                "SUCCESS",
                metadataJson
        ));
    }

    private String metadata(
            String key1,
            String value1,
            String key2,
            String value2,
            String key3,
            String value3,
            String key4,
            String value4,
            String key5,
            String value5
    ) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\",\""
                + key4 + "\":\"" + safeValue(value4) + "\",\""
                + key5 + "\":\"" + safeValue(value5) + "\"}";
    }

    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsentServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consent.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.consent.dao.ConsentDao;
import com.saneb.domain.consent.dto.ConsentSaveRequest;
import com.saneb.domain.consent.dto.CurrentConsentResponse;
import com.saneb.domain.consent.dto.UserConsentResponse;
import com.saneb.domain.consent.service.ConsentService;
import com.saneb.domain.consent.vo.ConsentVersionRow;
import com.saneb.domain.consent.vo.UserConsentInsertCommand;
import com.saneb.domain.consent.vo.UserConsentRow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentServiceImpl implements ConsentService {

    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final String TERMS_OF_SERVICE = "TERMS_OF_SERVICE";
    private static final String PRIVACY_POLICY = "PRIVACY_POLICY";
    private static final Set<String> CONSENT_CODES = Set.of(
            TERMS_OF_SERVICE,
            PRIVACY_POLICY,
            "E_CERT",
            "CREDIT_CHECK"
    );

    private final ConsentDao consentDao;

    /**
     * 객체를 생성합니다.
     *
     * @param consentDao 입력 값
     */
    public ConsentServiceImpl(ConsentDao consentDao) {
        this.consentDao = consentDao;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    @Override
    public List<CurrentConsentResponse> selectCurrentConsentList() {
        return consentDao.selectCurrentConsentVersionList().stream()
                .map(this::toCurrentConsentResponse)
                .toList();
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public List<UserConsentResponse> selectMyConsentList(Authentication authentication) {
        UUID userId = selectRequiredUserId(authentication);
        return consentDao.selectUserConsentList(userId).stream()
                .map(this::toUserConsentResponse)
                .toList();
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
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
     * @param httpRequest 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public UserConsentResponse insertMyConsent(
            Authentication authentication,
            ConsentSaveRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID userId = selectRequiredUserId(authentication);
        String consentCode = normalizeConsentCode(request.consentCode());
        ConsentVersionRow version = selectCurrentVersion(consentCode);
        UUID userConsentId = insertConsent(userId, version, true, httpRequest);
        return toUserConsentResponse(consentDao.selectUserConsentDetails(userConsentId));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param userId 입력 값
     *
     * @param httpRequest 입력 값
     */
    @Override
    public void insertSignupRequiredConsents(UUID userId, HttpServletRequest httpRequest) {
        insertConsent(userId, selectCurrentVersion(TERMS_OF_SERVICE), true, httpRequest);
        insertConsent(userId, selectCurrentVersion(PRIVACY_POLICY), true, httpRequest);
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param userId 입력 값
     *
     * @param version 입력 값
     *
     * @param consented 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @return 처리 결과
     */
    private UUID insertConsent(
            UUID userId,
            ConsentVersionRow version,
            boolean consented,
            HttpServletRequest httpRequest
    ) {
        return consentDao.insertUserConsent(new UserConsentInsertCommand(
                userId,
                version.consentVersionId(),
                version.consentCode(),
                consented,
                selectClientIpAddress(httpRequest),
                truncateUserAgent(httpRequest.getHeader("User-Agent"))
        ));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param consentCode 입력 값
     *
     * @return 처리 결과
     */
    private ConsentVersionRow selectCurrentVersion(String consentCode) {
        ConsentVersionRow version = consentDao.selectCurrentConsentVersionDetailsByCode(consentCode);
        if (version == null) {
            throw new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "현재 동의 항목을 찾을 수 없습니다."
            );
        }
        return version;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    private UUID selectRequiredUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal.userId();
        }
        throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeConsentCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase();
        if (!CONSENT_CODES.contains(code)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "동의 항목이 올바르지 않습니다."
            );
        }
        return code;
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private CurrentConsentResponse toCurrentConsentResponse(ConsentVersionRow row) {
        return new CurrentConsentResponse(
                row.consentVersionId(),
                row.consentCode(),
                row.consentName(),
                row.versionNo(),
                row.required(),
                row.effectiveFrom()
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private UserConsentResponse toUserConsentResponse(UserConsentRow row) {
        return new UserConsentResponse(
                row.userConsentId(),
                row.consentVersionId(),
                row.consentCode(),
                row.consentName(),
                row.versionNo(),
                row.consented(),
                row.consentedAt()
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param httpRequest 입력 값
     *
     * @return 처리 결과
     */
    private String selectClientIpAddress(HttpServletRequest httpRequest) {
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param userAgent 입력 값
     *
     * @return 처리 결과
     */
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}

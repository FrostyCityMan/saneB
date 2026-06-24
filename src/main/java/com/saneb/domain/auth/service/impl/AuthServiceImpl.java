/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuthServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.dao.AuthDao;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.dto.LoginRequest;
import com.saneb.domain.auth.dto.LoginResponse;
import com.saneb.domain.auth.dto.PasswordChangeRequest;
import com.saneb.domain.auth.dto.SignupRequest;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.auth.vo.AuthLoginHistoryCommand;
import com.saneb.domain.auth.vo.AuthPasswordUpdateCommand;
import com.saneb.domain.auth.vo.AuthSignupCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.consent.service.ConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROUTE = "/app/dashboard";
    private static final String ADMIN_ROUTE = "/app/admin/dashboard";
    private static final String APPROVER_ROUTE = "/app/approver/reviews";
    private static final String OPERATOR_ROUTE = "/app/operator/dashboard";
    private static final String REVIEWER_ROUTE = "/app/reviewer/dashboard";
    private static final String PARTNER_ROUTE = "/app/partner/verifications";
    private static final String PASSWORD_ROUTE = "/password";
    private static final String DEFAULT_SIGNUP_ROLE = "USER";
    private static final int MAX_USER_AGENT_LENGTH = 500;

    private final AuthDao authDao;
    private final ConsentService consentService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    /**
     * 객체를 생성합니다.
     *
     * @param authDao 입력 값
     *
     * @param consentService 입력 값
     *
     * @param passwordEncoder 입력 값
     *
     * @param securityContextRepository 입력 값
     */
    public AuthServiceImpl(
            AuthDao authDao,
            ConsentService consentService,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository
    ) {
        this.authDao = authDao;
        this.consentService = consentService;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     *
     * @return 처리 결과
     */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthUserDetailsRow user = authDao.selectAuthUserDetailsByLoginId(request.loginId());
        if (user == null) {
            insertLoginHistory(null, request.loginId(), "FAIL", httpRequest, "USER_NOT_FOUND");
            throw invalidCredentials();
        }

        if (!"ACTIVE".equals(user.statusCode())) {
            insertLoginHistory(user.userId(), user.loginId(), "FAIL", httpRequest, user.statusCode());
            throw new ApiException(ErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN, "사용할 수 없는 계정입니다.");
        }

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            insertLoginHistory(user.userId(), user.loginId(), "FAIL", httpRequest, "BAD_CREDENTIALS");
            throw invalidCredentials();
        }

        AuthenticatedUserDetails principal = toPrincipal(user);
        saveAuthentication(principal, httpRequest, httpResponse);

        authDao.updateUserLastLoginAt(user.userId());
        insertLoginHistory(user.userId(), user.loginId(), "SUCCESS", httpRequest, null);
        return LoginResponse.from(toAuthMeResponse(principal));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     *
     * @return 처리 결과
     */
    @Override
    /**
     * 업무 처리를 수행합니다.
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     *
     * @return 처리 결과
     */
    @Transactional
    public LoginResponse signup(
            SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String loginId = normalizeRequired(request.loginId());
        String name = normalizeRequired(request.name());
        String phone = nullIfBlank(request.phone());
        String email = nullIfBlank(request.email());

        validateSignupRequest(request);

        if (authDao.selectAuthUserDetailsByLoginId(loginId) != null) {
            throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID, HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (phone != null && authDao.selectUserIdByPhone(phone) != null) {
            throw new ApiException(ErrorCode.DUPLICATE_PHONE, HttpStatus.CONFLICT, "이미 사용 중인 휴대폰 번호입니다.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        UUID userId = authDao.insertUser(new AuthSignupCommand(
                loginId,
                passwordHash,
                name,
                phone,
                email
        ));
        authDao.insertUserRole(userId, DEFAULT_SIGNUP_ROLE);
        consentService.insertSignupRequiredConsents(userId, httpRequest);

        AuthUserDetailsRow user = new AuthUserDetailsRow(
                userId,
                loginId,
                passwordHash,
                name,
                "ACTIVE",
                false,
                null,
                null,
                null
        );
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user, List.of(DEFAULT_SIGNUP_ROLE));
        saveAuthentication(principal, httpRequest, httpResponse);
        authDao.updateUserLastLoginAt(userId);
        insertLoginHistory(userId, loginId, "SUCCESS", httpRequest, null);
        return LoginResponse.from(toAuthMeResponse(principal));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     */
    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(emptyContext, httpRequest, httpResponse);

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    @Override
    public AuthMeResponse selectAuthMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return toAuthMeResponse(principal);
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .sorted(Comparator.comparingInt(this::selectRolePriority))
                .toList();
        String primaryRole = roles.isEmpty() ? "USER" : roles.get(0);

        return new AuthMeResponse(
                null,
                authentication.getName(),
                authentication.getName(),
                roles,
                primaryRole,
                selectDefaultRoute(primaryRole),
                false,
                new AuthMeResponse.ProfileResponse(null, null, null)
        );
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     */
    @Override
    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param request 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     */
    @Transactional
    public void updatePassword(
            Authentication authentication,
            PasswordChangeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthenticatedUserDetails principal = selectCurrentPrincipal(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), principal.getPassword())) {
            throw invalidCredentials();
        }

        String passwordHash = passwordEncoder.encode(request.newPassword());
        authDao.updatePassword(new AuthPasswordUpdateCommand(
                principal.userId(),
                passwordHash
        ));
        saveAuthentication(withUpdatedPassword(principal, passwordHash), httpRequest, httpResponse);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails selectCurrentPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal;
        }

        AuthUserDetailsRow user = authDao.selectAuthUserDetailsByLoginId(authentication.getName());
        if (user == null) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return toPrincipal(user);
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param user 입력 값
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails toPrincipal(AuthUserDetailsRow user) {
        List<String> roles = authDao.selectRoleCodeListByUserId(user.userId()).stream()
                .sorted(Comparator.comparingInt(this::selectRolePriority))
                .toList();
        return new AuthenticatedUserDetails(user, roles.isEmpty() ? List.of("USER") : roles);
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param principal 입력 값
     *
     * @return 처리 결과
     */
    private AuthMeResponse toAuthMeResponse(AuthenticatedUserDetails principal) {
        List<String> roles = principal.roles().stream()
                .sorted(Comparator.comparingInt(this::selectRolePriority))
                .toList();
        String primaryRole = roles.isEmpty() ? "USER" : roles.get(0);

        return new AuthMeResponse(
                principal.userId(),
                principal.loginId(),
                principal.name(),
                roles,
                primaryRole,
                principal.passwordResetRequired() ? PASSWORD_ROUTE : selectDefaultRoute(primaryRole),
                principal.passwordResetRequired(),
                new AuthMeResponse.ProfileResponse(
                        principal.memberProfileId(),
                        principal.businessProfileId(),
                        principal.partnerProfileId()
                )
        );
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param userId 입력 값
     *
     * @param loginId 입력 값
     *
     * @param loginResultCode 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param failureReasonCode 입력 값
     */
    private void insertLoginHistory(
            java.util.UUID userId,
            String loginId,
            String loginResultCode,
            HttpServletRequest httpRequest,
            String failureReasonCode
    ) {
        authDao.insertAuthLoginHistory(new AuthLoginHistoryCommand(
                userId,
                loginId,
                loginResultCode,
                selectClientIpAddress(httpRequest),
                truncateUserAgent(httpRequest.getHeader("User-Agent")),
                failureReasonCode
        ));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param principal 입력 값
     *
     * @param passwordHash 입력 값
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails withUpdatedPassword(AuthenticatedUserDetails principal, String passwordHash) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        principal.userId(),
                        principal.loginId(),
                        passwordHash,
                        principal.name(),
                        principal.statusCode(),
                        false,
                        principal.memberProfileId(),
                        principal.businessProfileId(),
                        principal.partnerProfileId()
                ),
                principal.roles()
        );
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param principal 입력 값
     *
     * @param httpRequest 입력 값
     *
     * @param httpResponse 입력 값
     */
    private void saveAuthentication(
            AuthenticatedUserDetails principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param request 입력 값
     */
    private void validateSignupRequest(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다.");
        }
        if (!Boolean.TRUE.equals(request.termsAgreed()) || !Boolean.TRUE.equals(request.privacyAgreed())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "회원가입 약관에 동의해 주세요.");
        }
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private ApiException invalidCredentials() {
        return new ApiException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED,
                "로그인 정보가 올바르지 않습니다."
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param primaryRole 입력 값
     *
     * @return 처리 결과
     */
    private String selectDefaultRoute(String primaryRole) {
        return switch (primaryRole) {
            case "ADMIN" -> ADMIN_ROUTE;
            case "APPROVER" -> APPROVER_ROUTE;
            case "OPERATOR" -> OPERATOR_ROUTE;
            case "REVIEWER" -> REVIEWER_ROUTE;
            case "PARTNER" -> PARTNER_ROUTE;
            default -> DEFAULT_ROUTE;
        };
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param role 입력 값
     *
     * @return 처리 결과
     */
    private int selectRolePriority(String role) {
        return switch (role) {
            case "ADMIN" -> 1;
            case "APPROVER" -> 2;
            case "OPERATOR" -> 3;
            case "REVIEWER" -> 4;
            case "PARTNER" -> 5;
            default -> 6;
        };
    }
}

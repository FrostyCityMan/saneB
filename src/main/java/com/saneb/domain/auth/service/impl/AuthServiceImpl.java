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
    private static final String PASSWORD_ROUTE = "/password";
    private static final String DEFAULT_SIGNUP_ROLE = "USER";
    private static final int MAX_USER_AGENT_LENGTH = 500;

    private final AuthDao authDao;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    public AuthServiceImpl(
            AuthDao authDao,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository
    ) {
        this.authDao = authDao;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
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

    @Override
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

    @Override
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

    private AuthenticatedUserDetails toPrincipal(AuthUserDetailsRow user) {
        List<String> roles = authDao.selectRoleCodeListByUserId(user.userId()).stream()
                .sorted(Comparator.comparingInt(this::selectRolePriority))
                .toList();
        return new AuthenticatedUserDetails(user, roles.isEmpty() ? List.of("USER") : roles);
    }

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

    private void validateSignupRequest(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다.");
        }
        if (!Boolean.TRUE.equals(request.termsAgreed()) || !Boolean.TRUE.equals(request.privacyAgreed())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "회원가입 약관에 동의해 주세요.");
        }
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String selectClientIpAddress(HttpServletRequest httpRequest) {
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }

    private ApiException invalidCredentials() {
        return new ApiException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED,
                "로그인 정보가 올바르지 않습니다."
        );
    }

    private String selectDefaultRoute(String primaryRole) {
        return switch (primaryRole) {
            case "ADMIN" -> ADMIN_ROUTE;
            default -> DEFAULT_ROUTE;
        };
    }

    private int selectRolePriority(String role) {
        return switch (role) {
            case "ADMIN" -> 1;
            case "APPROVER" -> 2;
            case "OPERATOR" -> 3;
            case "PARTNER" -> 4;
            default -> 5;
        };
    }
}

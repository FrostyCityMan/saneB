package com.saneb.domain.auth.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auth.dao.AuthDao;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.dto.LoginRequest;
import com.saneb.domain.auth.dto.LoginResponse;
import com.saneb.domain.auth.dto.PasswordChangeRequest;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.auth.vo.AuthLoginHistoryCommand;
import com.saneb.domain.auth.vo.AuthPasswordUpdateCommand;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
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
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        authDao.updateUserLastLoginAt(user.userId());
        insertLoginHistory(user.userId(), user.loginId(), "SUCCESS", httpRequest, null);
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
                DEFAULT_ROUTE,
                false,
                new AuthMeResponse.ProfileResponse(null, null, null)
        );
    }

    @Override
    @Transactional
    public void updatePassword(Authentication authentication, PasswordChangeRequest request) {
        AuthenticatedUserDetails principal = selectCurrentPrincipal(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), principal.getPassword())) {
            throw invalidCredentials();
        }

        authDao.updatePassword(new AuthPasswordUpdateCommand(
                principal.userId(),
                passwordEncoder.encode(request.newPassword())
        ));
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
                DEFAULT_ROUTE,
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

package com.saneb.domain.auth.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.dto.LoginRequest;
import com.saneb.domain.auth.dto.LoginResponse;
import com.saneb.domain.auth.dto.PasswordChangeRequest;
import com.saneb.domain.auth.dto.SignupRequest;
import com.saneb.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ApiResponse.success(authService.login(request, httpRequest, httpResponse));
    }

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ApiResponse.success(authService.signup(request, httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> selectAuthMe(Authentication authentication) {
        return ApiResponse.success(authService.selectAuthMe(authentication));
    }

    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authService.updatePassword(authentication, request, httpRequest, httpResponse);
        return ApiResponse.success(null);
    }
}

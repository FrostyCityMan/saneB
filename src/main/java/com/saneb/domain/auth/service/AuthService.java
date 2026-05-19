package com.saneb.domain.auth.service;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.dto.LoginRequest;
import com.saneb.domain.auth.dto.LoginResponse;
import com.saneb.domain.auth.dto.PasswordChangeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    AuthMeResponse selectAuthMe(Authentication authentication);

    void updatePassword(Authentication authentication, PasswordChangeRequest request);
}

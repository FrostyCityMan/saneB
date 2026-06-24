/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuthViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auth.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

@Controller
public class AuthViewController {

    private final AuthService authService;

    public AuthViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String selectLoginPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            AuthMeResponse authMe = authService.selectAuthMe(authentication);
            return "redirect:" + authMe.defaultRoute();
        }
        return "auth/login";
    }

    @GetMapping("/signup")
    public String selectSignupPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            AuthMeResponse authMe = authService.selectAuthMe(authentication);
            return "redirect:" + authMe.defaultRoute();
        }
        return "auth/signup";
    }

    @GetMapping("/password")
    public String selectPasswordPage() {
        return "auth/password";
    }

    @GetMapping("/invalid-access")
    public String selectInvalidAccessPage(
            @RequestParam(name = "reason", required = false) String reason,
            Model model
    ) {
        boolean forbidden = "forbidden".equals(reason);
        model.addAttribute("title", forbidden ? "접근 권한이 없습니다" : "로그인이 필요합니다");
        model.addAttribute(
                "message",
                forbidden
                        ? "현재 계정으로는 요청하신 화면에 접근할 수 없습니다."
                        : "로그인 정보가 만료되었거나 인증이 필요한 화면입니다."
        );
        model.addAttribute("detail", "다시 로그인한 뒤 필요한 업무를 이어서 진행해 주세요.");
        return "auth/invalid-access";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}

package com.saneb.domain.auth.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}

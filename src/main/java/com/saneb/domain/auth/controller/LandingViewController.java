package com.saneb.domain.auth.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingViewController {

    private final AuthService authService;

    public LandingViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String redirectLanding(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }

        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        return "redirect:" + authMe.defaultRoute();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}

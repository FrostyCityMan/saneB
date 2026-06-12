package com.saneb.domain.candidatepreview.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CandidatePreviewViewController {

    private final AuthService authService;

    public CandidatePreviewViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/candidate-preview")
    public String selectCandidatePreviewPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            AuthMeResponse authMe = authService.selectAuthMe(authentication);
            return "redirect:" + authMe.defaultRoute();
        }
        return "auth/candidate-preview";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}

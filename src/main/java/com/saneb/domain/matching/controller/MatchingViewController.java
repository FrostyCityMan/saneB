package com.saneb.domain.matching.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MatchingViewController {

    private final AuthService authService;

    public MatchingViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/app/matching/cases")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public String selectMatchingCasePage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", MatchingCasePageModel.from(authMe));
        return "app/matching-cases";
    }

    public record MatchingCasePageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav
    ) {

        private static MatchingCasePageModel from(AuthMeResponse auth) {
            return new MatchingCasePageModel(
                    auth,
                    MatchingViewController.roleLabel(auth.primaryRole()),
                    "MATCHING_CASES"
            );
        }
    }

    private static String roleLabel(String code) {
        return switch (code) {
            case "ADMIN" -> "관리자";
            case "APPROVER" -> "승인자";
            case "OPERATOR" -> "운영자";
            case "PARTNER" -> "파트너";
            default -> "일반 사용자";
        };
    }
}

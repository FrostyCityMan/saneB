package com.saneb.domain.billing.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BillingViewController {

    private final AuthService authService;

    public BillingViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/app/billing/mock")
    @PreAuthorize("hasAnyRole('USER', 'OPERATOR', 'ADMIN')")
    public String selectMockBillingPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", MockBillingPageModel.from(authMe));
        return "app/mock-billing";
    }

    public record MockBillingPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav
    ) {

        private static MockBillingPageModel from(AuthMeResponse auth) {
            return new MockBillingPageModel(
                    auth,
                    BillingViewController.roleLabel(auth.primaryRole()),
                    "BILLING_MOCK"
            );
        }
    }

    private static String roleLabel(String code) {
        return switch (code) {
            case "ADMIN" -> "관리자";
            case "OPERATOR" -> "운영자";
            case "PARTNER" -> "파트너";
            case "REVIEWER" -> "검수자";
            default -> "일반 사용자";
        };
    }
}

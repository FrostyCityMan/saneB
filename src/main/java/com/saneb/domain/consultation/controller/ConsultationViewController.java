package com.saneb.domain.consultation.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConsultationViewController {

    private final AuthService authService;

    public ConsultationViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/app/consultations")
    @PreAuthorize("hasAnyRole('USER', 'PARTNER', 'OPERATOR', 'ADMIN')")
    public String selectConsultationPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", ConsultationPageModel.from(authMe));
        return "app/consultations";
    }

    public record ConsultationPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            boolean operating
    ) {

        private static ConsultationPageModel from(AuthMeResponse auth) {
            return new ConsultationPageModel(
                    auth,
                    ConsultationViewController.roleLabel(auth.primaryRole()),
                    "CONSULTATIONS",
                    auth.roles().contains("OPERATOR") || auth.roles().contains("ADMIN")
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

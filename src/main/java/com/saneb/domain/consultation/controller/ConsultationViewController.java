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
            String pageTitle,
            String summaryText,
            boolean operating
    ) {

        private static ConsultationPageModel from(AuthMeResponse auth) {
            boolean operating = auth.roles().contains("OPERATOR") || auth.roles().contains("ADMIN");
            return new ConsultationPageModel(
                    auth,
                    ConsultationViewController.roleLabel(auth.primaryRole()),
                    "CONSULTATIONS",
                    operating ? "상담 관리" : "상담 요청",
                    operating
                            ? "운영자는 요청 건을 확인해 담당자와 상태를 수동으로 배정합니다."
                            : "상담이 필요한 내용을 남기면 운영자가 확인한 뒤 담당자를 배정합니다.",
                    operating
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

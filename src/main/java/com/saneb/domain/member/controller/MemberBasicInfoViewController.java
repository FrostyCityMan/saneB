package com.saneb.domain.member.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MemberBasicInfoViewController {

    private final AuthService authService;

    public MemberBasicInfoViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/app/member/basic-info")
    public String selectBasicInfoPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", BasicInfoPageModel.from(authMe));
        return "app/member-basic-info";
    }

    public record BasicInfoPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav
    ) {

        private static BasicInfoPageModel from(AuthMeResponse auth) {
            return new BasicInfoPageModel(
                    auth,
                    MemberBasicInfoViewController.roleLabel(auth.primaryRole()),
                    "MEMBER_BASIC_INFO"
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

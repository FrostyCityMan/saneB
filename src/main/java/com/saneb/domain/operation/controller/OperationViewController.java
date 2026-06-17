package com.saneb.domain.operation.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OperationViewController {

    private final AuthService authService;

    public OperationViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/app/notifications")
    @PreAuthorize("isAuthenticated()")
    public String selectNotificationPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", PageModel.from(authMe, "알림", "NOTIFICATIONS", false));
        return "app/notifications";
    }

    @GetMapping("/app/operation-tasks")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public String selectOperationTaskPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", PageModel.from(authMe, "운영 업무 큐", "OPERATION_TASKS", true));
        return "app/operation-tasks";
    }

    public record PageModel(
            AuthMeResponse auth,
            String pageTitle,
            String roleLabel,
            String activeNav,
            boolean operating
    ) {

        private static PageModel from(AuthMeResponse auth, String pageTitle, String activeNav, boolean operating) {
            return new PageModel(auth, pageTitle, OperationViewController.roleLabel(auth.primaryRole()), activeNav, operating);
        }
    }

    private static String roleLabel(String code) {
        return switch (code) {
            case "ADMIN" -> "관리자";
            case "APPROVER" -> "승인자";
            case "OPERATOR" -> "운영자";
            case "REVIEWER" -> "검수자";
            case "PARTNER" -> "파트너";
            default -> "일반 사용자";
        };
    }
}

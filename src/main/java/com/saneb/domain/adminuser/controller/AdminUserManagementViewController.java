/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminUserManagementViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.adminuser.controller;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.dto.AdminRoleResponse;
import com.saneb.domain.adminuser.dto.AdminUserSummaryResponse;
import com.saneb.domain.adminuser.service.AdminUserManagementService;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminUserManagementViewController {

    private static final List<StatusOptionModel> STATUS_OPTIONS = List.of(
            new StatusOptionModel("ACTIVE", "사용 가능"),
            new StatusOptionModel("LOCKED", "잠김"),
            new StatusOptionModel("DISABLED", "사용 중지"),
            new StatusOptionModel("DELETED", "삭제 처리")
    );

    private final AuthService authService;
    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementViewController(
            AuthService authService,
            AdminUserManagementService adminUserManagementService
    ) {
        this.authService = authService;
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping("/app/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String selectAdminUserManagementPage(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PageResponse<AdminUserSummaryResponse> users = adminUserManagementService.selectUserList(
                keyword,
                statusCode,
                roleCode,
                page,
                size
        );
        List<AdminRoleResponse> roles = adminUserManagementService.selectRoleList();
        model.addAttribute("page", AdminUserManagementPageModel.from(
                authMe,
                users,
                roles,
                keyword,
                statusCode,
                roleCode,
                size
        ));
        return "app/admin-users";
    }

    public record AdminUserManagementPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            PageResponse<AdminUserSummaryResponse> users,
            List<AdminRoleResponse> roles,
            List<StatusOptionModel> statusOptions,
            String keyword,
            String statusCode,
            String roleCode,
            int size,
            int previousPage,
            int nextPage
    ) {

        private static AdminUserManagementPageModel from(
                AuthMeResponse auth,
                PageResponse<AdminUserSummaryResponse> users,
                List<AdminRoleResponse> roles,
                String keyword,
                String statusCode,
                String roleCode,
                int size
        ) {
            int previousPage = Math.max(1, users.page() - 1);
            int nextPage = users.totalPages() == 0 ? 1 : Math.min(users.totalPages(), users.page() + 1);
            return new AdminUserManagementPageModel(
                    auth,
                    AdminUserManagementViewController.roleLabel(auth.primaryRole()),
                    "ADMIN_USERS",
                    users,
                    roles,
                    STATUS_OPTIONS,
                    keyword == null ? "" : keyword,
                    statusCode == null ? "" : statusCode,
                    roleCode == null ? "" : roleCode,
                    size,
                    previousPage,
                    nextPage
            );
        }
    }

    public record StatusOptionModel(
            String statusCode,
            String label
    ) {
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

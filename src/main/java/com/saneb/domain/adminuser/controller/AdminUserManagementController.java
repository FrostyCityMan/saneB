/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminUserManagementController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.adminuser.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.dto.AdminRoleResponse;
import com.saneb.domain.adminuser.dto.AdminUserRolesUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserStatusUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserSummaryResponse;
import com.saneb.domain.adminuser.service.AdminUserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminUserSummaryResponse>> selectUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(adminUserManagementService.selectUserList(
                keyword,
                statusCode,
                roleCode,
                page,
                size
        ));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminRoleResponse>> selectRoleList() {
        return ApiResponse.success(adminUserManagementService.selectRoleList());
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserSummaryResponse> updateUserStatus(
            Authentication authentication,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        return ApiResponse.success(adminUserManagementService.updateUserStatus(authentication, userId, request));
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserSummaryResponse> updateUserRoles(
            Authentication authentication,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserRolesUpdateRequest request
    ) {
        return ApiResponse.success(adminUserManagementService.updateUserRoles(authentication, userId, request));
    }
}

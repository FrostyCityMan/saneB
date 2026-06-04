package com.saneb.domain.adminuser.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.dto.AdminRoleResponse;
import com.saneb.domain.adminuser.dto.AdminUserRolesUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserStatusUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AdminUserManagementService {

    PageResponse<AdminUserSummaryResponse> selectUserList(
            String keyword,
            String statusCode,
            String roleCode,
            int page,
            int size
    );

    List<AdminRoleResponse> selectRoleList();

    AdminUserSummaryResponse updateUserStatus(
            Authentication authentication,
            UUID userId,
            AdminUserStatusUpdateRequest request
    );

    AdminUserSummaryResponse updateUserRoles(
            Authentication authentication,
            UUID userId,
            AdminUserRolesUpdateRequest request
    );
}

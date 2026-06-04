package com.saneb.domain.adminuser.dto;

public record AdminRoleResponse(
        String roleCode,
        String roleName,
        int sortOrder
) {
}

package com.saneb.domain.adminuser.vo;

public record AdminUserSearchCondition(
        String keyword,
        String statusCode,
        String roleCode,
        int page,
        int size,
        int offset
) {
}

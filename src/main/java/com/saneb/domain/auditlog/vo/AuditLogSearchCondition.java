package com.saneb.domain.auditlog.vo;

public record AuditLogSearchCondition(
        String keyword,
        String actionCode,
        String resourceType,
        String resultCode,
        int page,
        int size,
        int offset
) {
}

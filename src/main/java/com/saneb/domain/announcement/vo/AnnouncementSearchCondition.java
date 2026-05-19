package com.saneb.domain.announcement.vo;

public record AnnouncementSearchCondition(
        String keyword,
        String targetTypeCode,
        String manualStatusCode,
        String approvalStatusCode,
        int page,
        int size,
        int offset
) {
}

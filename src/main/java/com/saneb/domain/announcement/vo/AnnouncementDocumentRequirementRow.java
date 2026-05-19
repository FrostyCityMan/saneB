package com.saneb.domain.announcement.vo;

public record AnnouncementDocumentRequirementRow(
        String documentTypeCode,
        Boolean required,
        Integer sortOrder
) {
}

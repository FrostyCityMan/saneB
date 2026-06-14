package com.saneb.domain.announcement.vo;

import java.util.UUID;

public record AnnouncementDocumentRequirementRow(
        String documentTypeCode,
        Boolean required,
        Integer sortOrder,
        UUID standardFieldId
) {
}

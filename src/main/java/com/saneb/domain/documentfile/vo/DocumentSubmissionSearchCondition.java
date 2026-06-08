package com.saneb.domain.documentfile.vo;

import java.util.UUID;

public record DocumentSubmissionSearchCondition(
        UUID submittedBy,
        String resourceTypeCode,
        UUID resourceId,
        String statusCode,
        int page,
        int size,
        int offset
) {
}

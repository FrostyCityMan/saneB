package com.saneb.domain.documentfile.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentSubmissionRow(
        UUID submissionId,
        UUID fileId,
        String originalFilename,
        String contentType,
        Long fileSize,
        String resourceTypeCode,
        UUID resourceId,
        String documentTypeCode,
        String statusCode,
        UUID submittedBy,
        OffsetDateTime submittedAt,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
        String reviewNote
) {
}

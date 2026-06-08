package com.saneb.domain.documentfile.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentSubmissionResponse(
        UUID submissionId,
        UUID fileId,
        String originalFilename,
        String contentType,
        long fileSize,
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

package com.saneb.domain.documentfile.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoredFileResponse(
        UUID fileId,
        UUID ownerUserId,
        String originalFilename,
        String contentType,
        long fileSize,
        String checksumSha256,
        String statusCode,
        OffsetDateTime createdAt
) {
}

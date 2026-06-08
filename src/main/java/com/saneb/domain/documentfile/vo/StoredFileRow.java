package com.saneb.domain.documentfile.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoredFileRow(
        UUID fileId,
        UUID ownerUserId,
        String originalFilename,
        String storedFilename,
        String storageKey,
        String contentType,
        Long fileSize,
        String checksumSha256,
        String statusCode,
        OffsetDateTime createdAt
) {
}

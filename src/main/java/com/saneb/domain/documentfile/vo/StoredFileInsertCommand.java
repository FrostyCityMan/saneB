package com.saneb.domain.documentfile.vo;

import java.util.UUID;

public record StoredFileInsertCommand(
        UUID fileId,
        UUID ownerUserId,
        String originalFilename,
        String storedFilename,
        String storageKey,
        String contentType,
        long fileSize,
        String checksumSha256,
        UUID actorUserId
) {
}

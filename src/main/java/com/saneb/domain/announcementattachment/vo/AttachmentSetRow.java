package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;
import java.time.OffsetDateTime;

public record AttachmentSetRow(UUID setId, UUID sourceId, UUID contentVersionId, UUID policyId,
                               String discoveryStatus, String setStatus, String manifestHash, String profileHash,
                               Integer discoveredCount, Integer processedCount, Boolean discoveryComplete,
                               OffsetDateTime createdAt, OffsetDateTime discoveredAt, OffsetDateTime sealedAt,
                               java.util.List<String> warningCodes) { }

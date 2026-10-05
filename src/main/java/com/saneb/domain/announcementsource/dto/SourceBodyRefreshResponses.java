package com.saneb.domain.announcementsource.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SourceBodyRefreshResponses {
    private SourceBodyRefreshResponses() { }
    public record Preview(UUID previewId, UUID sourceId, String beforeBody, String afterBody,
            String beforeStatusCode, String afterStatusCode, String reasonCode,
            List<String> targetCategoryCodes, List<String> supportTypeCodes,
            OffsetDateTime expiresAt, String bodyHash, boolean isChanged) { }
    public record Applied(UUID previewId, UUID sourceId, UUID evaluationId, String statusCode) { }
}

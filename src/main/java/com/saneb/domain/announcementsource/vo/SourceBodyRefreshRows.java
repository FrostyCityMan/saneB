package com.saneb.domain.announcementsource.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class SourceBodyRefreshRows {
    private SourceBodyRefreshRows() { }
    public record Boundary(UUID registeredSourceId, String registeredSourceUrl, boolean blocked) { }
    public record Preview(UUID id, UUID sourceId, UUID requestedBy, UUID baseEvaluationId,
            UUID contentVersionId, UUID ruleReleaseId, int sourceVersion, int attachmentVersion,
            String bodyText, String bodyHash, String extractorVersion, OffsetDateTime expiresAt,
            UUID appliedEvaluationId) { }
}

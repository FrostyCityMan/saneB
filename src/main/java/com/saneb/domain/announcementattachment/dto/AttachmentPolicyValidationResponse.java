package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AttachmentPolicyValidationResponse(UUID runId, UUID policyId, Integer policyVersion, UUID ruleReleaseId,
        Integer ruleVersion, String snapshotHash, String statusCode, Integer rowVersion, boolean inputVersionsCurrent,
        String errorCode, OffsetDateTime createdAt, OffsetDateTime startedAt, OffsetDateTime completedAt, List<Step> steps) {
    public AttachmentPolicyValidationResponse { steps = List.copyOf(steps); }
    public record Step(String stepCode, String statusCode, Map<String,Object> evidence, String evidenceHash) {
        public Step { evidence = Map.copyOf(evidence); }
    }
}

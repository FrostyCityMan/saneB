package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AttachmentPolicyResponses {
    private AttachmentPolicyResponses() { }
    public record Summary(UUID policyId,String policyCode,Integer versionNo,Integer rowVersion,String policyStatusCode,
            String modeCode,UUID ruleReleaseId,String ruleReleaseStatusCode,String policyHash,
            OffsetDateTime createdAt,OffsetDateTime publishedAt) { }
    public record Configuration(String engineVersion,String extractorVersion,String extractorConfigHash,Long maximumSourceBytes,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String roleRuleVersion,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String roleRulesHash) {
        public Configuration(String engineVersion,String extractorVersion,String extractorConfigHash,Long maximumSourceBytes) {
            this(engineVersion,extractorVersion,extractorConfigHash,maximumSourceBytes,null,null);
        }
    }
    public record Profile(String providerCode,String profileCode,String profileHash) { }
    public record Details(Summary policy,Configuration configuration,List<Profile> systemProfileBindings,
            UUID copiedFromPolicyId,boolean isEditable,boolean isDraftValidationRequired,OffsetDateTime updatedAt) {
        public Details { systemProfileBindings=List.copyOf(systemProfileBindings); }
    }
}

package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public final class AttachmentPolicyPublicationScopeRows {
    private AttachmentPolicyPublicationScopeRows() { }
    public record Request(UUID scopeId,UUID policyId,UUID requestedBy,String requestHash) {
        @Override public String toString() { return "PublicationScopeRequest[id="+scopeId+"]"; }
    }
    public record Insert(UUID scopeId,UUID policyId,int expectedVersion,UUID actorId,UUID key,String requestHash,String reasonHash) { }
    public record Search(UUID policyId,UUID scopeId,int size,int offset) { }
}

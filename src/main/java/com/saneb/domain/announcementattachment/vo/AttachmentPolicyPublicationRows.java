package com.saneb.domain.announcementattachment.vo;

import java.util.UUID;

public final class AttachmentPolicyPublicationRows {
    private AttachmentPolicyPublicationRows(){ }
    public record Request(UUID publicationId,UUID policyId,UUID actorId,String requestHash){
        @Override public String toString(){return "PolicyPublicationRequest[id="+publicationId+"]";}
    }
    public record Insert(UUID publicationId,UUID policyId,UUID scopeId,UUID actorId,UUID key,String requestHash,String reasonHash,
            String evidenceHash,String runtimeHash){ }
}

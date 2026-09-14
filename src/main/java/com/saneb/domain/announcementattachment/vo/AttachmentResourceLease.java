package com.saneb.domain.announcementattachment.vo;

import java.util.List;
import java.util.UUID;

public record AttachmentResourceLease(UUID jobId, UUID jobLeaseToken, List<UUID> resourceTokens) {
    public AttachmentResourceLease { resourceTokens = List.copyOf(resourceTokens); }
}

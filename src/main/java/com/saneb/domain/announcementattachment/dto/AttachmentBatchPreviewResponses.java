package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.*;

public final class AttachmentBatchPreviewResponses {
    private AttachmentBatchPreviewResponses() { }
    public record Preview(UUID previewId,UUID batchId,String statusCode,String scopeHash,String inputHash,String previewHash,
            int snapshotBatchVersion,int currentBatchVersion,int itemCount,int snapshotRemainingItemCount,int snapshotDeletedItemCount,
            int availableItemCount,int currentDeletedItemCount,int eligibleItemCount,int selectedItemCount,
            boolean currentPreview,boolean inputsCurrent,int currentHttpRequests,OffsetDateTime createdAt) { }
    public record Item(UUID jobId,UUID sourceId,String providerCode,String readinessCode,boolean eligible,boolean selected,
            Map<String,Object> evidence) { }
}

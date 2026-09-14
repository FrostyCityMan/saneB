package com.saneb.domain.announcementattachment.vo;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBatchPreviewRows {
    private AttachmentBatchPreviewRows() { }
    /** 고정 지문 계산용 내부 metadata. 제목/본문/추출 텍스트/URL은 조회하지 않는다. */
    public record LiveItem(UUID jobId,UUID sourceId,String providerCode,String jobStatusCode,String jobErrorCode,
            Boolean evidenceComplete,Boolean frozenSourceCurrent,Boolean protectedLink,Boolean activeOtherJob,
            String inputJson,String evidenceJson) { }
    public record Preview(UUID previewId,UUID batchId,String statusCode,String scopeHash,String inputHash,String previewHash,
            Integer batchVersion,Integer itemCount,Integer remainingItemCount,Integer deletedItemCount,Integer eligibleItemCount,
            Integer selectedItemCount,UUID actorId,UUID idempotencyKey,String requestHash,String reasonHash,OffsetDateTime createdAt) { }
    public record ItemInsert(UUID previewId,UUID batchId,UUID jobId,String readinessCode,boolean eligible,boolean selected,String inputHash,String evidenceJson) { }
    public record Item(UUID jobId,UUID sourceId,String providerCode,String readinessCode,Boolean eligible,Boolean selected,String inputHash,String evidenceJson) { }
    public record Search(UUID previewId,int size,int offset) { }
}

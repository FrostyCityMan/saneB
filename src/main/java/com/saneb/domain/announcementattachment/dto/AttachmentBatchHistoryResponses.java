package com.saneb.domain.announcementattachment.dto;

import com.saneb.common.response.PageResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AttachmentBatchHistoryResponses {
    private AttachmentBatchHistoryResponses() { }
    /** 승인 당시 불변 범위다. 현재 처리 건수는 기존 개별 영수증 API에서 별도로 읽는다. */
    public record Entry(UUID actionId,UUID batchId,String actionKind,String actionCode,Integer acceptedFromVersion,
            UUID previewId,Integer scopeItemCount,Integer approvedTargetCount,Integer deletedCountAtAcceptance,
            Integer approvedEligibleCount,Integer approvedBaseReopenCount,Integer approvedConfirmationRestoreCount,
            Integer cancelledPendingCount,OffsetDateTime acceptedAt) { }
    public record History(UUID batchId,int throughVersion,int currentBatchVersion,PageResponse<Entry> history,int currentHttpRequests) { }
}

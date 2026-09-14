package com.saneb.domain.announcementattachment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 요청 접수 영수증과 현재 실제 건수를 구분한다. 적용 성공을 접수 응답으로 대체하지 않는다. */
public record AttachmentBatchApplicationResponse(UUID actionId,UUID batchId,UUID previewId,String actionCode,
        int acceptedFromVersion,String currentStatusCode,int currentVersion,int scopeItemCount,int approvedSelectedCount,
        int remainingItemCount,int deletedItemCount,int selectedRemainingCount,int pendingCount,int appliedCount,
        int conflictCount,int failedCount,int currentHttpRequests,OffsetDateTime acceptedAt) { }

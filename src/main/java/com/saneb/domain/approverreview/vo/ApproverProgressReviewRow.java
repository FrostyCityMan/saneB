package com.saneb.domain.approverreview.vo;

public record ApproverProgressReviewRow(
        int waitingResultCount,
        int approvedCount,
        int supplementRequestedCount,
        int stoppedCount
) {
}

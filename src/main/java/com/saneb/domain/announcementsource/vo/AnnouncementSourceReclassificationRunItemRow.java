package com.saneb.domain.announcementsource.vo;

import java.util.UUID;

public record AnnouncementSourceReclassificationRunItemRow(
        UUID itemId,
        UUID runId,
        UUID sourceId,
        UUID contentVersionId,
        String contentHash,
        int expectedClassificationVersion,
        UUID previousEvaluationId,
        String previousSemanticStatusCode,
        String previousSemanticReasonCode,
        String previousSemanticMatchedKeywords,
        String previousReviewStatusCode,
        String predictedSemanticStatusCode,
        String predictedReasonCode,
        String predictionHash,
        String itemStatusCode,
        UUID appliedEvaluationId,
        Integer appliedClassificationVersion
) {
}

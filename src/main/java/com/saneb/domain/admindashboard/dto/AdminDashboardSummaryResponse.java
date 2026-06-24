/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminDashboardSummaryResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.admindashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardSummaryResponse(
        UserSummaryResponse userSummary,
        AnnouncementSummaryResponse announcementSummary,
        VerificationSummaryResponse verificationSummary,
        MatchingSummaryResponse matchingSummary,
        ApplicationProgressSummaryResponse applicationProgressSummary,
        AuditSummaryResponse auditSummary
) {

    public record UserSummaryResponse(
            int totalUserCount,
            int activeUserCount,
            int userRoleCount,
            int partnerRoleCount,
            int operatorRoleCount,
            int approverRoleCount,
            int adminRoleCount
    ) {
    }

    public record AnnouncementSummaryResponse(
            int totalAnnouncementCount,
            int draftCount,
            int requestedCount,
            int approvedCount,
            int rejectedCount,
            int openAnnouncementCount,
            int pausedAnnouncementCount,
            int closedAnnouncementCount
    ) {
    }

    public record VerificationSummaryResponse(
            int totalVerificationCount,
            int reviewQueueCount,
            int verifiedCount,
            int rejectedCount,
            List<StatusCountResponse> statusCounts
    ) {
    }

    public record MatchingSummaryResponse(
            int totalMatchingCaseCount,
            int matchedCount,
            int reviewRequiredCount,
            int blockedCount,
            int progressedCount,
            List<StatusCountResponse> statusCounts
    ) {
    }

    public record ApplicationProgressSummaryResponse(
            int totalProgressCount,
            int activeProgressCount,
            int waitingResultCount,
            int approvedCount,
            int supplementRequestedCount,
            int stoppedCount,
            int completedCount,
            BigDecimal totalReceivedAmount,
            List<StatusCountResponse> statusCounts
    ) {
    }

    public record AuditSummaryResponse(
            int totalAuditCount,
            int failAuditCount,
            int recentFailAuditCount
    ) {
    }

    public record StatusCountResponse(
            String statusCode,
            int count
    ) {
    }
}

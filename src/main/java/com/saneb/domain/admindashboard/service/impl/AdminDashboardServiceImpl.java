package com.saneb.domain.admindashboard.service.impl;

import com.saneb.domain.admindashboard.dao.AdminDashboardDao;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.ApplicationProgressSummaryResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.AnnouncementSummaryResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.AuditSummaryResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.MatchingSummaryResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.StatusCountResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.UserSummaryResponse;
import com.saneb.domain.admindashboard.dto.AdminDashboardSummaryResponse.VerificationSummaryResponse;
import com.saneb.domain.admindashboard.service.AdminDashboardService;
import com.saneb.domain.admindashboard.vo.AdminApplicationProgressSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminAnnouncementSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminAuditSummaryRow;
import com.saneb.domain.admindashboard.vo.AdminStatusCountRow;
import com.saneb.domain.admindashboard.vo.AdminUserSummaryRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final List<String> PARTNER_VERIFICATION_STATUSES = List.of(
            "DRAFT",
            "SUBMITTED",
            "REVIEWING",
            "VERIFIED",
            "REJECTED",
            "EXPIRED"
    );
    private static final List<String> MATCHING_CASE_STATUSES = List.of(
            "MATCHED",
            "NOT_MATCHED",
            "REVIEW_REQUIRED",
            "BLOCKED",
            "PROGRESSED"
    );
    private static final List<String> APPLICATION_PROGRESS_STATUSES = List.of(
            "READY",
            "IN_PROGRESS",
            "WAITING_RESULT",
            "APPROVED",
            "REJECTED",
            "SUPPLEMENT_REQUESTED",
            "STOPPED",
            "COMPLETED"
    );

    private final AdminDashboardDao adminDashboardDao;

    public AdminDashboardServiceImpl(AdminDashboardDao adminDashboardDao) {
        this.adminDashboardDao = adminDashboardDao;
    }

    @Override
    public AdminDashboardSummaryResponse selectSummary() {
        AdminUserSummaryRow userSummary = nullToEmpty(adminDashboardDao.selectUserSummary());
        AdminAnnouncementSummaryRow announcementSummary = nullToEmpty(adminDashboardDao.selectAnnouncementSummary());
        List<StatusCountResponse> verificationStatusCounts = selectStatusCounts(
                adminDashboardDao.selectPartnerVerificationStatusCountList(),
                PARTNER_VERIFICATION_STATUSES
        );
        List<StatusCountResponse> matchingStatusCounts = selectStatusCounts(
                adminDashboardDao.selectMatchingCaseStatusCountList(),
                MATCHING_CASE_STATUSES
        );
        AdminApplicationProgressSummaryRow progressSummary =
                nullToEmpty(adminDashboardDao.selectApplicationProgressSummary());
        AdminAuditSummaryRow auditSummary = nullToEmpty(adminDashboardDao.selectAuditSummary());

        return new AdminDashboardSummaryResponse(
                new UserSummaryResponse(
                        userSummary.totalUserCount(),
                        userSummary.activeUserCount(),
                        userSummary.userRoleCount(),
                        userSummary.partnerRoleCount(),
                        userSummary.operatorRoleCount(),
                        userSummary.approverRoleCount(),
                        userSummary.adminRoleCount()
                ),
                new AnnouncementSummaryResponse(
                        announcementSummary.totalAnnouncementCount(),
                        announcementSummary.draftCount(),
                        announcementSummary.requestedCount(),
                        announcementSummary.approvedCount(),
                        announcementSummary.rejectedCount(),
                        announcementSummary.openAnnouncementCount(),
                        announcementSummary.pausedAnnouncementCount(),
                        announcementSummary.closedAnnouncementCount()
                ),
                new VerificationSummaryResponse(
                        sumStatusCount(verificationStatusCounts),
                        countFor(verificationStatusCounts, "SUBMITTED") + countFor(verificationStatusCounts, "REVIEWING"),
                        countFor(verificationStatusCounts, "VERIFIED"),
                        countFor(verificationStatusCounts, "REJECTED"),
                        verificationStatusCounts
                ),
                new MatchingSummaryResponse(
                        sumStatusCount(matchingStatusCounts),
                        countFor(matchingStatusCounts, "MATCHED"),
                        countFor(matchingStatusCounts, "REVIEW_REQUIRED"),
                        countFor(matchingStatusCounts, "BLOCKED"),
                        countFor(matchingStatusCounts, "PROGRESSED"),
                        matchingStatusCounts
                ),
                new ApplicationProgressSummaryResponse(
                        progressSummary.totalProgressCount(),
                        progressSummary.readyCount() + progressSummary.inProgressCount(),
                        progressSummary.waitingResultCount(),
                        progressSummary.approvedCount(),
                        progressSummary.supplementRequestedCount(),
                        progressSummary.stoppedCount(),
                        progressSummary.completedCount(),
                        progressSummary.totalReceivedAmount() == null
                                ? BigDecimal.ZERO
                                : progressSummary.totalReceivedAmount(),
                        progressStatusCounts(progressSummary)
                ),
                new AuditSummaryResponse(
                        auditSummary.totalAuditCount(),
                        auditSummary.failAuditCount(),
                        auditSummary.recentFailAuditCount()
                )
        );
    }

    private List<StatusCountResponse> selectStatusCounts(List<AdminStatusCountRow> rows, List<String> statusCodes) {
        Map<String, AdminStatusCountRow> rowByStatusCode = rows == null
                ? Map.of()
                : rows.stream().collect(Collectors.toMap(AdminStatusCountRow::statusCode, Function.identity()));
        return statusCodes.stream()
                .map(statusCode -> new StatusCountResponse(
                        statusCode,
                        rowByStatusCode.getOrDefault(statusCode, new AdminStatusCountRow(statusCode, 0)).count()
                ))
                .toList();
    }

    private List<StatusCountResponse> progressStatusCounts(AdminApplicationProgressSummaryRow row) {
        return List.of(
                new StatusCountResponse("READY", row.readyCount()),
                new StatusCountResponse("IN_PROGRESS", row.inProgressCount()),
                new StatusCountResponse("WAITING_RESULT", row.waitingResultCount()),
                new StatusCountResponse("APPROVED", row.approvedCount()),
                new StatusCountResponse("REJECTED", row.rejectedCount()),
                new StatusCountResponse("SUPPLEMENT_REQUESTED", row.supplementRequestedCount()),
                new StatusCountResponse("STOPPED", row.stoppedCount()),
                new StatusCountResponse("COMPLETED", row.completedCount())
        );
    }

    private int sumStatusCount(List<StatusCountResponse> statusCounts) {
        return statusCounts.stream()
                .mapToInt(StatusCountResponse::count)
                .sum();
    }

    private int countFor(List<StatusCountResponse> statusCounts, String statusCode) {
        return statusCounts.stream()
                .filter(statusCount -> statusCode.equals(statusCount.statusCode()))
                .mapToInt(StatusCountResponse::count)
                .findFirst()
                .orElse(0);
    }

    private AdminUserSummaryRow nullToEmpty(AdminUserSummaryRow row) {
        return row == null ? new AdminUserSummaryRow(0, 0, 0, 0, 0, 0, 0) : row;
    }

    private AdminAnnouncementSummaryRow nullToEmpty(AdminAnnouncementSummaryRow row) {
        return row == null ? new AdminAnnouncementSummaryRow(0, 0, 0, 0, 0, 0, 0, 0) : row;
    }

    private AdminApplicationProgressSummaryRow nullToEmpty(AdminApplicationProgressSummaryRow row) {
        return row == null ? new AdminApplicationProgressSummaryRow(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO
        ) : row;
    }

    private AdminAuditSummaryRow nullToEmpty(AdminAuditSummaryRow row) {
        return row == null ? new AdminAuditSummaryRow(0, 0, 0) : row;
    }
}

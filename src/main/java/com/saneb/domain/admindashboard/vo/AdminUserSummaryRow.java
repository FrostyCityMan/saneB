package com.saneb.domain.admindashboard.vo;

public record AdminUserSummaryRow(
        int totalUserCount,
        int activeUserCount,
        int userRoleCount,
        int partnerRoleCount,
        int operatorRoleCount,
        int approverRoleCount,
        int adminRoleCount
) {
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuditLogViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auditlog.controller;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auditlog.dto.AuditLogDetailsResponse;
import com.saneb.domain.auditlog.dto.AuditLogSummaryResponse;
import com.saneb.domain.auditlog.service.AuditLogService;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditLogViewController {

    private static final List<OptionModel> RESOURCE_OPTIONS = List.of(
            new OptionModel("USER", "회원"),
            new OptionModel("PARTNER_VERIFICATION", "검증"),
            new OptionModel("MATCHING_CASE", "매칭"),
            new OptionModel("APPLICATION_PROGRESS", "신청 진행")
    );
    private static final List<OptionModel> RESULT_OPTIONS = List.of(
            new OptionModel("SUCCESS", "성공"),
            new OptionModel("FAIL", "실패")
    );

    private final AuthService authService;
    private final AuditLogService auditLogService;

    public AuditLogViewController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/app/audit-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public String selectAuditLogListPage(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resultCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        PageResponse<AuditLogSummaryResponse> auditLogs = auditLogService.selectAuditLogList(
                keyword,
                actionCode,
                resourceType,
                resultCode,
                page,
                size
        );
        model.addAttribute("page", AuditLogListPageModel.from(
                authMe,
                auditLogs,
                keyword,
                actionCode,
                resourceType,
                resultCode,
                size
        ));
        return "app/audit-logs";
    }

    @GetMapping("/app/audit-logs/{auditLogId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public String selectAuditLogDetailsPage(
            Authentication authentication,
            Model model,
            @PathVariable UUID auditLogId
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        AuditLogDetailsResponse auditLog = auditLogService.selectAuditLogDetails(auditLogId);
        model.addAttribute("page", AuditLogDetailsPageModel.from(authMe, auditLog));
        return "app/audit-log-details";
    }

    public record AuditLogListPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            PageResponse<AuditLogSummaryResponse> auditLogs,
            List<OptionModel> resourceOptions,
            List<OptionModel> resultOptions,
            String keyword,
            String actionCode,
            String resourceType,
            String resultCode,
            int size,
            int previousPage,
            int nextPage
    ) {

        private static AuditLogListPageModel from(
                AuthMeResponse auth,
                PageResponse<AuditLogSummaryResponse> auditLogs,
                String keyword,
                String actionCode,
                String resourceType,
                String resultCode,
                int size
        ) {
            int previousPage = Math.max(1, auditLogs.page() - 1);
            int nextPage = auditLogs.totalPages() == 0 ? 1 : Math.min(auditLogs.totalPages(), auditLogs.page() + 1);
            return new AuditLogListPageModel(
                    auth,
                    AuditLogViewController.roleLabel(auth.primaryRole()),
                    "AUDIT_LOGS",
                    auditLogs,
                    RESOURCE_OPTIONS,
                    RESULT_OPTIONS,
                    keyword == null ? "" : keyword,
                    actionCode == null ? "" : actionCode,
                    resourceType == null ? "" : resourceType,
                    resultCode == null ? "" : resultCode,
                    size,
                    previousPage,
                    nextPage
            );
        }
    }

    public record AuditLogDetailsPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            AuditLogDetailsResponse auditLog
    ) {

        private static AuditLogDetailsPageModel from(AuthMeResponse auth, AuditLogDetailsResponse auditLog) {
            return new AuditLogDetailsPageModel(
                    auth,
                    AuditLogViewController.roleLabel(auth.primaryRole()),
                    "AUDIT_LOGS",
                    auditLog
            );
        }
    }

    public record OptionModel(
            String value,
            String label
    ) {
    }

    private static String roleLabel(String code) {
        return switch (code) {
            case "ADMIN" -> "관리자";
            case "APPROVER" -> "승인자";
            case "OPERATOR" -> "운영자";
            case "PARTNER" -> "파트너";
            default -> "일반 사용자";
        };
    }
}

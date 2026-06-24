/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DashboardController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dashboard.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.dashboard.dto.DashboardCurrentActionResponse;
import com.saneb.domain.dashboard.dto.DashboardProgressSummaryResponse;
import com.saneb.domain.dashboard.dto.DashboardReverificationStatusResponse;
import com.saneb.domain.dashboard.dto.DashboardSummaryResponse;
import com.saneb.domain.dashboard.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/me")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> selectMySummary(Authentication authentication) {
        return ApiResponse.success(dashboardService.selectMySummary(authentication));
    }

    @GetMapping("/current-action")
    public ApiResponse<DashboardCurrentActionResponse> selectMyCurrentAction(Authentication authentication) {
        return ApiResponse.success(dashboardService.selectMyCurrentAction(authentication));
    }

    @GetMapping("/progress-summary")
    public ApiResponse<DashboardProgressSummaryResponse> selectMyProgressSummary(Authentication authentication) {
        return ApiResponse.success(dashboardService.selectMyProgressSummary(authentication));
    }

    @GetMapping("/reverification-status")
    public ApiResponse<DashboardReverificationStatusResponse> selectMyReverificationStatus(Authentication authentication) {
        return ApiResponse.success(dashboardService.selectMyReverificationStatus(authentication));
    }
}

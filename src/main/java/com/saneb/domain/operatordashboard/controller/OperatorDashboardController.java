package com.saneb.domain.operatordashboard.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.operatordashboard.dto.OperatorDashboardSummaryResponse;
import com.saneb.domain.operatordashboard.service.OperatorDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operator/dashboard")
public class OperatorDashboardController {

    private final OperatorDashboardService operatorDashboardService;

    public OperatorDashboardController(OperatorDashboardService operatorDashboardService) {
        this.operatorDashboardService = operatorDashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ApiResponse<OperatorDashboardSummaryResponse> selectSummary() {
        return ApiResponse.success(operatorDashboardService.selectSummary());
    }
}

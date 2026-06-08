package com.saneb.domain.applog.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.applog.dto.AppLogResponse;
import com.saneb.domain.applog.service.AppLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/app-logs")
public class AppLogController {

    private final AppLogService appLogService;

    public AppLogController(AppLogService appLogService) {
        this.appLogService = appLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AppLogResponse> selectAppLog(
            @RequestParam(required = false) String levelCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "120") int lines
    ) {
        return ApiResponse.success(appLogService.selectAppLog(levelCode, keyword, lines));
    }
}

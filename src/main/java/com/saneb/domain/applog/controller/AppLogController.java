/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AppLogController.java
 * 작성자: 김도훈
 *
 */

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

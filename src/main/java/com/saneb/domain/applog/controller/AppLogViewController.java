/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AppLogViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.applog.controller;

import com.saneb.domain.applog.dto.AppLogResponse;
import com.saneb.domain.applog.service.AppLogService;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AppLogViewController {

    private static final List<LevelOptionModel> LEVEL_OPTIONS = List.of(
            new LevelOptionModel("", "전체"),
            new LevelOptionModel("ERROR", "오류"),
            new LevelOptionModel("WARN", "주의"),
            new LevelOptionModel("INFO", "정보"),
            new LevelOptionModel("DEBUG", "개발 확인")
    );
    private static final List<Integer> LINE_OPTIONS = List.of(80, 120, 200, 500);

    private final AuthService authService;
    private final AppLogService appLogService;

    public AppLogViewController(AuthService authService, AppLogService appLogService) {
        this.authService = authService;
        this.appLogService = appLogService;
    }

    @GetMapping("/app/admin/app-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public String selectAppLogPage(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) String levelCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "120") int lines
    ) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        AppLogResponse appLog = appLogService.selectAppLog(levelCode, keyword, lines);
        model.addAttribute("page", AppLogPageModel.from(authMe, appLog, keyword));
        return "app/admin-app-logs";
    }

    public record AppLogPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            AppLogResponse log,
            List<LevelOptionModel> levelOptions,
            List<Integer> lineOptions,
            String keyword
    ) {

        private static AppLogPageModel from(AuthMeResponse auth, AppLogResponse log, String keyword) {
            return new AppLogPageModel(
                    auth,
                    AppLogViewController.roleLabel(auth.primaryRole()),
                    "APP_LOGS",
                    log,
                    LEVEL_OPTIONS,
                    LINE_OPTIONS,
                    keyword == null ? "" : keyword
            );
        }
    }

    public record LevelOptionModel(
            String levelCode,
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

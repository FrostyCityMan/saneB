/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcement.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnnouncementViewController {

    private final AuthService authService;

    public AnnouncementViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/app/announcements/input")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public String selectAnnouncementInputPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", AnnouncementInputPageModel.from(authMe));
        return "app/announcement-input";
    }

    public record AnnouncementInputPageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav
    ) {

        private static AnnouncementInputPageModel from(AuthMeResponse auth) {
            return new AnnouncementInputPageModel(
                    auth,
                    AnnouncementViewController.roleLabel(auth.primaryRole()),
                    "ANNOUNCEMENT_INPUT"
            );
        }
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

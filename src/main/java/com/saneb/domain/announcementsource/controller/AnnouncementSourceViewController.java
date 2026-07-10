/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementSourceViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcementsource.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnnouncementSourceViewController {

    private final AuthService authService;

    /**
     * 객체를 생성합니다.
     *
     * @param authService 입력 값
     */
    public AnnouncementSourceViewController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 업무 화면을 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @param model 입력 값
     *
     * @return 처리 결과
     */
    @GetMapping("/app/admin/announcement-sources")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public String selectAnnouncementSourcePage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", AnnouncementSourcePageModel.from(authMe));
        return "app/announcement-sources";
    }

    public record AnnouncementSourcePageModel(
            AuthMeResponse auth,
            String roleLabel,
            String activeNav,
            String pageTitle
    ) {

        /**
         * 업무 데이터를 응답 형식으로 변환합니다.
         *
         * @param auth 입력 값
         *
         * @return 처리 결과
         */
        private static AnnouncementSourcePageModel from(AuthMeResponse auth) {
            return new AnnouncementSourcePageModel(
                    auth,
                    AnnouncementSourceViewController.roleLabel(auth.primaryRole()),
                    "ANNOUNCEMENT_SOURCES",
                    "API 공고 수집"
            );
        }
    }

    /**
     * 역할 라벨을 조회합니다.
     *
     * @param code 입력 값
     *
     * @return 처리 결과
     */
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

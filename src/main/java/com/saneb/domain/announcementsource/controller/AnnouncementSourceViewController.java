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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnnouncementSourceViewController {

    private final AuthService authService;
    private final boolean classificationV2Enabled;

    /**
     * 객체를 생성합니다.
     *
     * @param authService 입력 값
     */
    public AnnouncementSourceViewController(
            AuthService authService,
            @Value("${saneb.announcement-source.classification-v2.enabled:false}")
            boolean classificationV2Enabled
    ) {
        this.authService = authService;
        this.classificationV2Enabled = classificationV2Enabled;
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
        model.addAttribute("page", AnnouncementSourcePageModel.from(
                authMe,
                "ANNOUNCEMENT_SOURCES",
                "API 공고 수집"
        ));
        return "app/announcement-sources";
    }

    /**
     * 수집된 공고만 조회하고 검수하는 관리자 화면을 조회합니다.
     *
     * @param authentication 인증 정보
     * @param model 화면 모델
     * @return 수집 공고 검수 화면
     */
    @GetMapping("/app/admin/collected-announcements")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public String selectCollectedAnnouncementPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", AnnouncementSourcePageModel.from(
                authMe,
                "COLLECTED_ANNOUNCEMENTS",
                "수집 공고 검수"
        ));
        model.addAttribute("classificationV2Enabled", classificationV2Enabled);
        return "app/collected-announcements";
    }

    /**
     * 공고 분류 규칙 버전과 키워드를 조회하고 관리하는 화면을 조회합니다.
     *
     * @param authentication 인증 정보
     * @param model 화면 모델
     * @return 공고 키워드 관리 화면
     */
    @GetMapping("/app/admin/announcement-keywords")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public String selectAnnouncementKeywordPage(Authentication authentication, Model model) {
        AuthMeResponse authMe = authService.selectAuthMe(authentication);
        model.addAttribute("page", AnnouncementSourcePageModel.from(
                authMe,
                "ANNOUNCEMENT_KEYWORDS",
                "공고 키워드 관리"
        ));
        return "app/announcement-keywords";
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
         * @param auth 인증 정보
         * @param activeNav 활성 메뉴
         * @param pageTitle 화면 제목
         *
         * @return 처리 결과
         */
        private static AnnouncementSourcePageModel from(
                AuthMeResponse auth,
                String activeNav,
                String pageTitle
        ) {
            return new AnnouncementSourcePageModel(
                    auth,
                    AnnouncementSourceViewController.roleLabel(auth.primaryRole()),
                    activeNav,
                    pageTitle
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

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: CandidatePreviewViewController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.candidatepreview.controller;

import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CandidatePreviewViewController {

    private final AuthService authService;

    public CandidatePreviewViewController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/candidate-preview")
    public String selectCandidatePreviewPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            AuthMeResponse authMe = authService.selectAuthMe(authentication);
            return "redirect:" + authMe.defaultRoute();
        }
        return "auth/candidate-preview";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}

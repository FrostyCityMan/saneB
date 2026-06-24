/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ConsentController.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.consent.controller;

import com.saneb.common.response.ApiResponse;
import com.saneb.domain.consent.dto.ConsentSaveRequest;
import com.saneb.domain.consent.dto.CurrentConsentResponse;
import com.saneb.domain.consent.dto.UserConsentResponse;
import com.saneb.domain.consent.service.ConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/api/v1/consents/current")
    public ApiResponse<List<CurrentConsentResponse>> selectCurrentConsentList() {
        return ApiResponse.success(consentService.selectCurrentConsentList());
    }

    @GetMapping("/api/v1/users/me/consents")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<UserConsentResponse>> selectMyConsentList(Authentication authentication) {
        return ApiResponse.success(consentService.selectMyConsentList(authentication));
    }

    @PostMapping("/api/v1/users/me/consents")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserConsentResponse> insertMyConsent(
            Authentication authentication,
            @Valid @RequestBody ConsentSaveRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(consentService.insertMyConsent(authentication, request, httpRequest));
    }
}

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

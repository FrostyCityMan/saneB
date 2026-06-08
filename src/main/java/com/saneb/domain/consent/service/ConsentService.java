package com.saneb.domain.consent.service;

import com.saneb.domain.consent.dto.ConsentSaveRequest;
import com.saneb.domain.consent.dto.CurrentConsentResponse;
import com.saneb.domain.consent.dto.UserConsentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface ConsentService {

    List<CurrentConsentResponse> selectCurrentConsentList();

    List<UserConsentResponse> selectMyConsentList(Authentication authentication);

    UserConsentResponse insertMyConsent(
            Authentication authentication,
            ConsentSaveRequest request,
            HttpServletRequest httpRequest
    );

    void insertSignupRequiredConsents(UUID userId, HttpServletRequest httpRequest);
}

package com.saneb.domain.dynamicinput.service;

import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsResponse;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsSaveRequest;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesResponse;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesSaveRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface DynamicAnnouncementInputService {

    AnnouncementInputRequirementsResponse selectAnnouncementInputRequirements(UUID announcementId);

    AnnouncementInputRequirementsResponse saveAnnouncementInputRequirements(
            Authentication authentication,
            UUID announcementId,
            AnnouncementInputRequirementsSaveRequest request
    );

    ApplicationInputValuesResponse selectApplicationInputValues(Authentication authentication, UUID progressId);

    ApplicationInputValuesResponse saveApplicationInputValues(
            Authentication authentication,
            UUID progressId,
            ApplicationInputValuesSaveRequest request
    );
}

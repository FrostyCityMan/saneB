package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceClassificationDetailsResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementSourceReclassificationService {

    AnnouncementSourceClassificationDetailsResponse insertReclassification(
            Authentication authentication,
            UUID sourceId,
            AnnouncementSourceReclassificationRequest request
    );
}

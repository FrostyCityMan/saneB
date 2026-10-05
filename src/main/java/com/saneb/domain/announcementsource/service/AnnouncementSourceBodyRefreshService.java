package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.dto.SourceBodyRefreshResponses;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementSourceBodyRefreshService {
    SourceBodyRefreshResponses.Preview insertPreview(Authentication authentication, UUID sourceId);
    SourceBodyRefreshResponses.Applied savePreview(Authentication authentication, UUID sourceId, UUID previewId);
}

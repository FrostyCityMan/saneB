package com.saneb.domain.announcementsource.service;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceBaseReviewRequest;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementSourceBaseReviewService {
    AnnouncementSourceLinkResponse insertReviewedAnnouncement(Authentication authentication, UUID sourceId,
            AnnouncementSourceBaseReviewRequest request);
}

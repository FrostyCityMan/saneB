package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentReviewRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentReviewResponses;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceLinkResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentReviewService {
    AttachmentReviewResponses.Context selectReviewContextDetails(UUID sourceId);
    AttachmentReviewResponses.Confirmation insertConfirmation(Authentication authentication, UUID sourceId,
            UUID idempotencyKey, AttachmentReviewRequests.Confirmation request);
    AnnouncementSourceLinkResponse insertOperationalAnnouncement(Authentication authentication, UUID sourceId,
            AttachmentReviewRequests.Conversion request);
}

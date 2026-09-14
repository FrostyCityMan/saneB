package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentCollectionContext;
import com.saneb.domain.announcementattachment.dto.AttachmentCollectionRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentCollectionService {
    AttachmentCollectionContext selectCollectionContextDetails(Authentication authentication,UUID sourceId);
    AttachmentJobResponse insertCollectionJob(Authentication authentication,UUID sourceId,UUID key,AttachmentCollectionRequests.Request request);
}

package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentRoleRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentRoleService {
    AttachmentJobResponse insertRoleChange(Authentication authentication,UUID sourceId,UUID idempotencyKey,AttachmentRoleRequest request);
    AttachmentJobResponse selectJobDetails(UUID sourceId,UUID jobId);
}

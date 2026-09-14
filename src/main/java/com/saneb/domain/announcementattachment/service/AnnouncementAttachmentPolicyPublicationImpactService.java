package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationImpact;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentPolicyPublicationImpactService {
    AttachmentPolicyPublicationImpact selectImpactDetails(Authentication actor,UUID policyId);
}

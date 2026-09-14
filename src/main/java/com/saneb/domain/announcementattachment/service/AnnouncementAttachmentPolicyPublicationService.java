package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublication.*;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentPolicyPublicationService {
    Result insertPublication(Authentication actor,UUID policyId,UUID key,Request request);
    Result selectPublicationDetails(Authentication actor,UUID policyId);
}

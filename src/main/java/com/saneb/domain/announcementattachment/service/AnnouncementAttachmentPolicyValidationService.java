package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyValidationResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaPlanResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentPolicyValidationService {
    AttachmentProviderQaPlanResponse selectProviderQaPlan(Authentication actor,UUID policyId,int page,int size);
    PageResponse<AttachmentPolicyValidationResponse> selectRunList(Authentication actor, UUID policyId, int page, int size);
    AttachmentPolicyValidationResponse selectRunDetails(Authentication actor, UUID policyId, UUID runId);
    AttachmentPolicyValidationResponse insertRun(Authentication actor, UUID policyId, UUID key, AttachmentPolicyCheckRequest request);
    AttachmentPolicyValidationResponse updateCancellation(Authentication actor, UUID policyId, UUID runId, AttachmentPolicyCheckRequest request);
    String saveNextValidationRun();
}

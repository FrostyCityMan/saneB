package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentPolicyService {
    PageResponse<AttachmentPolicyResponses.Summary> selectPolicyList(Authentication actor,String status,UUID ruleReleaseId,int page,int size);
    AttachmentPolicyResponses.Details selectPolicyDetails(Authentication actor,UUID policyId);
    AttachmentPolicyResponses.Details insertPolicy(Authentication actor,UUID key,AttachmentPolicyRequests.Create request);
    AttachmentPolicyResponses.Details updatePolicyDraft(Authentication actor,UUID policyId,AttachmentPolicyRequests.Update request);
    AttachmentPolicyResponses.Details insertPolicyRevision(Authentication actor,UUID policyId,UUID key,AttachmentPolicyRequests.Revision request);
}

package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaRequests;
import com.saneb.domain.announcementattachment.dto.AttachmentProviderQaResponses.*;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentProviderQaManagementService {
    Preview selectExecutionPlan(Authentication actor,UUID policyId,int page,int size);
    Coverage selectTargetCoverageList(Authentication actor,UUID policyId,int page,int size);
    Run insertRun(Authentication actor,UUID policyId,UUID key,AttachmentProviderQaRequests.Reservation request);
    PageResponse<Run> selectRunList(Authentication actor,UUID policyId,int page,int size);
    Run selectRunDetails(Authentication actor,UUID policyId,UUID runId);
    PageResponse<Item> selectCaseList(Authentication actor,UUID policyId,UUID runId,int page,int size);
    Run updateCancellation(Authentication actor,UUID policyId,UUID runId,AttachmentPolicyCheckRequest request);
    String saveNextProviderQaRun();
}

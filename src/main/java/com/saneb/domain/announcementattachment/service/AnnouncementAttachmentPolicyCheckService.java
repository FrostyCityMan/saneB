package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyCheckResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentPolicyCheckService {
    PageResponse<AttachmentPolicyCheckResponse> selectCheckList(Authentication actor,UUID policyId,int page,int size);
    AttachmentPolicyCheckResponse insertClassificationCheck(Authentication actor,UUID policyId,UUID key,AttachmentPolicyCheckRequest request);
}

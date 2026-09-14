package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.*;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentPolicyPublicationScopeService {
    Details insertScope(Authentication actor,UUID policyId,UUID key,Prepare request);
    Details selectScopeDetails(Authentication actor,UUID policyId,UUID scopeId);
    PageResponse<Summary> selectScopeList(Authentication actor,UUID policyId,int page,int size);
    PageResponse<Item> selectItemList(Authentication actor,UUID policyId,UUID scopeId,int page,int size);
}

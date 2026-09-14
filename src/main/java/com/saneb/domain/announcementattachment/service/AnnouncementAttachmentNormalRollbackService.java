package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackRequest;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackResponses.*;
import com.saneb.common.response.PageResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentNormalRollbackService {
    PageResponse<JobSummary> selectJobList(Authentication actor,UUID sourceId,int page,int size);
    Preview selectPreviewDetails(Authentication actor,UUID sourceId,UUID jobId);
    Receipt insertRollback(Authentication actor,UUID sourceId,UUID jobId,UUID key,AttachmentNormalRollbackRequest request);
    Receipt selectActionDetails(Authentication actor,UUID sourceId,UUID jobId,UUID actionId);
}

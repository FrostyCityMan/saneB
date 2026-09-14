package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentBatchPreviewService {
    AttachmentBatchPreviewResponses.Preview insertPreview(Authentication actor,UUID batchId,UUID key,AttachmentBatchPreviewRequests.Preparation request);
    AttachmentBatchPreviewResponses.Preview updateSelection(Authentication actor,UUID batchId,UUID key,AttachmentBatchPreviewRequests.Selection request);
    AttachmentBatchPreviewResponses.Preview selectCurrentPreviewDetails(Authentication actor,UUID batchId);
    AttachmentBatchPreviewResponses.Preview selectPreviewDetails(Authentication actor,UUID batchId,UUID previewId);
    PageResponse<AttachmentBatchPreviewResponses.Item> selectItemList(Authentication actor,UUID batchId,UUID previewId,int page,int size);
}

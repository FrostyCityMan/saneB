package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchRollbackResponses.*;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentBatchRollbackService {
    Preview selectPreviewDetails(Authentication actor,UUID batchId);
    PageResponse<Item> selectItemList(Authentication actor,UUID batchId,int page,int size);
    Receipt insertRollback(Authentication actor,UUID batchId,UUID key,AttachmentBatchRollbackRequest request);
    Receipt selectActionDetails(Authentication actor,UUID batchId,UUID actionId);
    boolean saveNextRollback();
}

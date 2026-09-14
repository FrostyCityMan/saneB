package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchApplicationRows;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentBatchApplicationService {
    AttachmentBatchApplicationResponse insertAction(Authentication actor,UUID batchId,UUID key,String actionCode,AttachmentBatchApplicationRequest request);
    AttachmentBatchApplicationResponse selectActionDetails(Authentication actor,UUID batchId,UUID actionId);
    PageResponse<AttachmentBatchApplicationRows.Item> selectItemList(Authentication actor,UUID batchId,int page,int size);
    boolean saveNextApplication();
}

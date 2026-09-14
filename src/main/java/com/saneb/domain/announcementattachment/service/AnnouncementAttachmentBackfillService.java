package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.AttachmentBackfillRows;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentBackfillService {
    AttachmentBackfillResponses.Preview selectScopePreview(Authentication actor,AttachmentBackfillRequests.Scope scope);
    AttachmentBackfillResponses.Inventory insertInventory(Authentication actor,UUID key,AttachmentBackfillRequests.Inventory request);
    AttachmentBackfillResponses.Inventory selectRunDetails(Authentication actor,UUID runId);
    PageResponse<AttachmentBackfillResponses.Inventory> selectRunList(Authentication actor,int page,int size);
    PageResponse<AttachmentBackfillRows.Segment> selectSegmentList(Authentication actor,UUID runId,int page,int size);
    PageResponse<AttachmentBackfillRows.Item> selectItemList(Authentication actor,UUID runId,long segmentNo,int page,int size);
}

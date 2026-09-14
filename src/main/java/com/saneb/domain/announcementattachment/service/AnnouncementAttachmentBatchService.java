package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.*;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchRows;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentBatchService {
    AttachmentBatchResponses.Preview selectScopePreview(Authentication actor,AttachmentBatchRequests.Scope scope);
    AttachmentBatchResponses.Batch insertBatch(Authentication actor,UUID key,AttachmentBatchRequests.Reservation request);
    AttachmentBatchResponses.Preview selectFixedScopePreview(Authentication actor,AttachmentBatchRequests.Scope scope,AttachmentBatchRows.FixedScope fixed);
    AttachmentBatchResponses.Batch insertFixedBatch(Authentication actor,UUID key,AttachmentBatchRequests.Reservation request,AttachmentBatchRows.FixedScope fixed);
    AttachmentBatchResponses.Batch selectBatchDetails(Authentication actor,UUID batchId);
    PageResponse<AttachmentBatchResponses.Batch> selectBatchList(Authentication actor,int page,int size);
    PageResponse<AttachmentBatchRows.Item> selectItemList(Authentication actor,UUID batchId,int page,int size);
    AttachmentBatchResponses.Batch updateScopeCancellation(Authentication actor,UUID batchId,AttachmentBatchRequests.Cancellation request);
    AttachmentBatchResponses.Batch updateCollectionStart(Authentication actor,UUID batchId,AttachmentBatchRequests.Collection request);
    AttachmentBatchResponses.Batch updateCollectionPause(Authentication actor,UUID batchId,AttachmentBatchRequests.Pause request);
    AttachmentBatchResponses.Batch updateCollectionResume(Authentication actor,UUID batchId,AttachmentBatchRequests.Collection request);
    int saveCollectionProgress();
}

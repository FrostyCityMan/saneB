package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentHistoryResponses;
import java.util.UUID;

public interface AnnouncementAttachmentHistoryService {
    PageResponse<AttachmentHistoryResponses.Summary> selectEvaluationList(UUID sourceId,int page,int size);
    AttachmentHistoryResponses.Details selectEvaluationDetails(UUID sourceId,UUID evaluationId);
    PageResponse<AttachmentHistoryResponses.Input> selectInputList(UUID sourceId,UUID evaluationId,int page,int size);
    PageResponse<AttachmentHistoryResponses.Match> selectMatchList(UUID sourceId,UUID evaluationId,UUID fileId,int page,int size);
}

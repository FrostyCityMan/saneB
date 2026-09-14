package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentSourceResponses;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceSearchCondition;
import java.util.UUID;

public interface AnnouncementAttachmentCurrentService {
    PageResponse<AttachmentSourceResponses.Summary> selectSourceList(AttachmentSourceSearchCondition condition);
    AttachmentSourceResponses.Details selectSourceDetails(UUID sourceId);
    AttachmentSourceResponses.Summary selectClassificationDetails(UUID sourceId);
}

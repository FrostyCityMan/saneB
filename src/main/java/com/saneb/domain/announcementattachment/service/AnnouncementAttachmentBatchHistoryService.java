package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.History;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentBatchHistoryService {
    History selectActionList(Authentication actor,UUID batchId,Integer throughVersion,int page,int size);
}

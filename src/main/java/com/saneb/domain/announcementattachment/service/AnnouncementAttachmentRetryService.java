package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentJobResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentRetryRequest;
import com.saneb.domain.announcementattachment.vo.AttachmentRetryFileRow;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentRetryService {
    AttachmentJobResponse insertFileRetry(Authentication authentication,UUID sourceId,UUID idempotencyKey,AttachmentRetryRequest request);
    List<AttachmentRetryFileRow> selectRetryFileList(UUID jobId,UUID leaseToken);
}

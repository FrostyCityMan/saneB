package com.saneb.domain.announcementattachment.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentEvidenceResponses;
import java.util.UUID;

public interface AnnouncementAttachmentReadService {
    PageResponse<AttachmentEvidenceResponses.SetSummary> selectAttachmentSetList(UUID sourceId, int page, int size);
    PageResponse<AttachmentEvidenceResponses.FileSummary> selectAttachmentFileList(UUID sourceId, UUID setId, int page, int size);
    PageResponse<AttachmentEvidenceResponses.Block> selectAttachmentBlockList(UUID sourceId, UUID extractionId,
            int page, int size, int textOffset, int textLimit);
}

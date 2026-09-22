package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.AttachmentSegmentAnalysisResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentSegmentService {
    AttachmentSegmentAnalysisResponse selectAnalysisDetails(UUID sourceId, UUID extractionId);
    AttachmentSegmentAnalysisResponse insertAnalysis(Authentication authentication, UUID sourceId, UUID extractionId);
}

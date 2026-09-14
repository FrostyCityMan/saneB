package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.dto.*;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AnnouncementAttachmentBackfillSegmentService {
    AttachmentBackfillSegmentResponses.Preview selectReservationPreview(Authentication actor,UUID runId,long segmentNo);
    AttachmentBackfillSegmentResponses.Reservation insertReservation(Authentication actor,UUID runId,long segmentNo,UUID key,AttachmentBackfillSegmentRequests.Reservation request);
    AttachmentBackfillSegmentResponses.Summary selectSummaryDetails(Authentication actor,UUID runId);
}

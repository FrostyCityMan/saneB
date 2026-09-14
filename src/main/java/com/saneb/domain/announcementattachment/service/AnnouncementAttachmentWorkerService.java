package com.saneb.domain.announcementattachment.service;

import java.util.UUID;

public interface AnnouncementAttachmentWorkerService {
    Outcome saveNextAttachmentJob();
    record Outcome(UUID jobId, String statusCode) { }
}

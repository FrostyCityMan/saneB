package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementAttachmentEvidenceService {
    Optional<AttachmentSetEvidence.File> selectFileCheckpoint(UUID jobId, UUID leaseToken, AttachmentSetEvidence.Locator locator);
    boolean saveFileCheckpoint(UUID jobId, UUID leaseToken, AttachmentSetEvidence.File file);
    Optional<AttachmentSetRow> saveAttachmentSet(UUID jobId, UUID leaseToken, AttachmentSetEvidence result);
    Optional<AttachmentSetRow> saveRetriedAttachmentSet(UUID jobId, UUID leaseToken, AttachmentSetEvidence selectedResult);
}

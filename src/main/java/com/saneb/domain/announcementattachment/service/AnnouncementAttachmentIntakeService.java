package com.saneb.domain.announcementattachment.service;

import com.saneb.domain.announcementattachment.vo.AttachmentCollectionPlan;
import java.util.UUID;

public interface AnnouncementAttachmentIntakeService {
    AttachmentCollectionPlan saveCollectionPlan(UUID runId,UUID ruleReleaseId);
    void saveCollectedSource(UUID sourceId,AttachmentCollectionPlan plan,boolean newSource);
}

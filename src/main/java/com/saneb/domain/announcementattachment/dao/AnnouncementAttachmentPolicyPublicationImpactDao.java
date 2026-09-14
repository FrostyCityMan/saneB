package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationImpact.Counts;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentPolicyPublicationImpactDao {
    Counts selectCounts(@Param("ruleReleaseId") UUID ruleReleaseId);
}

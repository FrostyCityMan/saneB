package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentPolicyRow;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentCollectionDao {
    AttachmentPolicyRow selectActivePolicyDetails(@Param("ruleReleaseId") UUID ruleReleaseId,@Param("lock") boolean lock);
    boolean selectManualRequestRateAllowed(@Param("sourceId") UUID sourceId);
}

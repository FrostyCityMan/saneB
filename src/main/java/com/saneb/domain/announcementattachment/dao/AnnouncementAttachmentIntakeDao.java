package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentCollectionPlan;
import com.saneb.domain.announcementattachment.vo.AttachmentWorkerSourceRow;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentIntakeDao {
    AttachmentCollectionPlan selectCollectionPlanDetails(@Param("runId") UUID runId);
    UUID selectActivePolicyId(@Param("ruleReleaseId") UUID ruleReleaseId);
    UUID selectUnmatchedEnforcePolicyId(@Param("ruleReleaseId") UUID ruleReleaseId);
    int insertCollectionPlan(AttachmentCollectionPlan plan);
    AttachmentWorkerSourceRow selectSourceLocatorDetails(@Param("sourceId") UUID sourceId);
    boolean selectProtectedLinkExists(@Param("sourceId") UUID sourceId);
    UUID selectActiveJobId(@Param("sourceId") UUID sourceId);
    boolean selectRecentCheckExists(@Param("sourceId") UUID sourceId);
    int updateSourceIntakeStatus(@Param("sourceId") UUID sourceId,@Param("statusCode") String statusCode);
}

package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.dto.AttachmentPolicyResponses;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyManagementRows;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentPolicyDao {
    List<AttachmentPolicyResponses.Summary> selectPolicyList(AttachmentPolicyManagementRows.Search condition);
    long selectPolicyCount(AttachmentPolicyManagementRows.Search condition);
    AttachmentPolicyManagementRows.Row selectPolicyDetails(@Param("policyId") UUID policyId,@Param("lock") boolean lock);
    AttachmentPolicyManagementRows.Row selectCreationDetails(@Param("key") UUID key);
    String selectCreationLock(@Param("key") UUID key);
    String selectFamilyLock(@Param("policyCode") String policyCode);
    int selectLatestVersion(@Param("policyCode") String policyCode);
    String selectRuleStatus(@Param("ruleReleaseId") UUID ruleReleaseId);
    int insertPolicy(AttachmentPolicyManagementRows.Insert command);
    int updatePolicyDraft(AttachmentPolicyManagementRows.Update command);
}

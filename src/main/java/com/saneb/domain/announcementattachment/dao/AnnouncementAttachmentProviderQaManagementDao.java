package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.*;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentProviderQaManagementDao {
    int insertPlan(PlanInsert command);
    Run selectRunDetails(@Param("runId") UUID runId);
    Run selectRequestDetails(@Param("key") UUID key);
    List<Run> selectRunList(Search search);
    long selectRunCount(@Param("policyId") UUID policyId);
    List<Item> selectCaseList(CaseSearch search);
    long selectCaseCount(@Param("runId") UUID runId);
    List<UUID> selectActiveRunIds();
    boolean selectCoolingDown(@Param("policyId") UUID policyId);
    UUID selectNextCaseId(@Param("runId") UUID runId);
    int updatePendingInputChanged(@Param("runId") UUID runId);
}

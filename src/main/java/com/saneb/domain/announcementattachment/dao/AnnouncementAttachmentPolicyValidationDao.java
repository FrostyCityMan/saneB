package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentPolicyValidationRows.*;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentPolicyValidationDao {
    String selectQueueLock();
    Run selectRunDetails(@Param("runId") UUID runId, @Param("lock") boolean lock);
    Run selectRequestDetails(@Param("key") UUID key);
    Run selectPendingDetails();
    long selectRunCount(Search search);
    List<Run> selectRunList(Search search);
    List<Step> selectStepList(@Param("runId") UUID runId);
    List<Target> selectTargetList();
    int selectActiveCount();
    int selectRecentCount(@Param("policyId") UUID policyId);
    boolean selectCoolingDown(@Param("policyId") UUID policyId);
    int insertRun(Insert request);
    int updateClaim(@Param("runId") UUID runId, @Param("leaseToken") UUID token);
    int insertExtractionLease(@Param("runId") UUID runId, @Param("leaseToken") UUID token);
    boolean selectExecutionAllowed(@Param("runId") UUID runId, @Param("leaseToken") UUID token);
    int insertStep(@Param("runId") UUID runId, @Param("leaseToken") UUID token, @Param("stepCode") String step,
            @Param("statusCode") String status, @Param("evidenceJson") String json, @Param("evidenceHash") String hash);
    int updateFinished(@Param("runId") UUID runId, @Param("leaseToken") UUID token, @Param("statusCode") String status, @Param("errorCode") String error);
    int updateCancellation(@Param("runId") UUID runId, @Param("expectedVersion") int version);
    int updateExpired();
    int deleteExtractionLease(@Param("runId") UUID runId, @Param("leaseToken") UUID token);
}

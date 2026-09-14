package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaRows.*;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentProviderQaDao {
    String selectQueueLock();
    String selectRunLock(@Param("runId") UUID runId);
    UUID selectRunId(@Param("caseId") UUID caseId);
    CaseRow selectCaseDetails(@Param("caseId") UUID caseId);
    List<CaseRow> selectCaseList(@Param("runId") UUID runId);
    int insertRun(RunInsert command);
    int insertCase(CaseInsert command);
    int updateReady(@Param("runId") UUID runId);
    int updateRunStarted(@Param("runId") UUID runId);
    int updateClaim(@Param("caseId") UUID caseId, @Param("leaseToken") UUID token);
    boolean selectExecutionAllowed(@Param("caseId") UUID caseId, @Param("leaseToken") UUID token);
    int updateUsage(@Param("caseId") UUID caseId, @Param("leaseToken") UUID token, @Param("requests") int requests, @Param("bytes") long bytes);
    int insertResource(Resource lease);
    int deleteResource(Resource lease);
    int deleteCaseResources(@Param("caseId") UUID caseId, @Param("leaseToken") UUID token);
    int updateFinished(@Param("caseId") UUID caseId, @Param("leaseToken") UUID token, @Param("statusCode") String status,
            @Param("errorCode") String error, @Param("evidenceJson") String json, @Param("evidenceHash") String hash);
    int updateCancellation(@Param("runId") UUID runId, @Param("expectedVersion") int version);
    int updateUnstartedCancelled(@Param("runId") UUID runId);
    int updateExpiredCases(@Param("runId") UUID runId);
    int updateRunFinished(@Param("runId") UUID runId);
}

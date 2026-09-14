package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentBatchRollbackRows.*;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AnnouncementAttachmentBatchRollbackDao {
    String selectRequestLock(@Param("key") UUID key);
    Action selectRequestDetails(@Param("key") UUID key);
    Action selectActionDetails(@Param("batchId") UUID batchId,@Param("actionId") UUID actionId);
    List<Candidate> selectCandidateList(@Param("batchId") UUID batchId);
    Candidate selectCandidateDetails(@Param("batchId") UUID batchId,@Param("jobId") UUID jobId);
    int insertAction(Action row);
    int insertTarget(Target row);
    int updateStart(@Param("batchId") UUID batchId,@Param("version") int version,@Param("actionId") UUID actionId);
    int updateJobsPending(@Param("batchId") UUID batchId,@Param("actionId") UUID actionId);
    int updateCancelApplication(@Param("batchId") UUID batchId);
    Work selectNextWorkDetails();
    Work selectWorkDetails(@Param("batchId") UUID batchId,@Param("jobId") UUID jobId);
    int insertConfirmationRestoration(Restoration row);
    int updateSourceRestoration(@Param("jobId") UUID jobId);
    int updateConfirmationCurrent(@Param("jobId") UUID jobId);
    int updateRolledBack(@Param("jobId") UUID jobId,@Param("confirmationRestored") boolean confirmationRestored);
    int updateConflict(@Param("jobId") UUID jobId,@Param("errorCode") String errorCode);
    int updateFailure(@Param("jobId") UUID jobId);
    int updateProgress(@Param("batchId") UUID batchId);
    int updateEmptyProgress();
    Counts selectCounts(@Param("actionId") UUID actionId);
}

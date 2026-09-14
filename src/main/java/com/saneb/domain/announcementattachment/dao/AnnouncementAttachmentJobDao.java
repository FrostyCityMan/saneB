package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentJobInsertCommand;
import com.saneb.domain.announcementattachment.vo.AttachmentJobRow;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyRow;
import com.saneb.domain.announcementattachment.vo.AttachmentSourceContextRow;
import com.saneb.domain.announcementattachment.vo.AttachmentWorkerSourceRow;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentJobDao {
    AttachmentSourceContextRow selectSourceContextDetailsForUpdate(@Param("sourceId") UUID sourceId);
    AttachmentSourceContextRow selectSourceContextDetails(@Param("sourceId") UUID sourceId);
    AttachmentPolicyRow selectPolicyDetails(@Param("policyId") UUID policyId);
    AttachmentJobRow selectIdempotentJobDetails(@Param("idempotencyKey") UUID idempotencyKey);
    AttachmentJobRow selectJobDetails(@Param("jobId") UUID jobId);
    AttachmentJobRow selectOwnedJobDetailsForUpdate(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken);
    AttachmentWorkerSourceRow selectWorkerSourceDetails(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken);
    boolean selectFrozenRunPolicyMatches(@Param("runId") UUID runId,@Param("policyId") UUID policyId,@Param("ruleReleaseId") UUID ruleReleaseId);
    boolean selectNewSourceBindingAllowed(@Param("sourceId") UUID sourceId,@Param("runId") UUID runId);
    UUID selectCurrentConfirmationId(@Param("sourceId") UUID sourceId);
    int updateNewSourceBinding(@Param("sourceId") UUID sourceId,@Param("policyId") UUID policyId,@Param("attachmentVersion") int attachmentVersion);
    boolean selectExternalExecutionAllowed(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken);
    boolean selectBatchExecutionUnchanged(@Param("jobId") UUID jobId);
    int updateJobDeferred(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken,
                          @Param("requestStarted") boolean requestStarted);
    int selectNextGeneration(@Param("sourceId") UUID sourceId,
                            @Param("contentVersionId") UUID contentVersionId, @Param("policyId") UUID policyId);
    int insertAttachmentJob(AttachmentJobInsertCommand command);
    int updateAttachmentSourceVersion(@Param("sourceId") UUID sourceId, @Param("expectedVersion") int expectedVersion);
    int updateAttachmentEvaluationsStale(@Param("sourceId") UUID sourceId);
    int updateAttachmentConfirmationsStale(@Param("sourceId") UUID sourceId);
    AttachmentJobRow updateNextJobLease(@Param("leaseToken") UUID leaseToken, @Param("leaseSeconds") int leaseSeconds);
    int updateExpiredJobLeases();
    int updateJobConflict(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken);
    int updateJobFrozenInputConflict(@Param("jobId") UUID jobId,@Param("leaseToken") UUID leaseToken);
    int updateJobHeartbeat(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken,
                           @Param("leaseSeconds") int leaseSeconds);
    int updateJobFailure(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken,
                        @Param("retryable") boolean retryable, @Param("errorCode") String errorCode,
                        @Param("jitterSeconds") int jitterSeconds);
    int updateJobDownloadBudget(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken,
                                @Param("bytes") long bytes);
    int insertResourceLease(@Param("resourceCode") String resourceCode, @Param("resourceKey") String resourceKey,
                            @Param("slotNo") int slotNo, @Param("jobId") UUID jobId,
                            @Param("jobLeaseToken") UUID jobLeaseToken, @Param("resourceLeaseToken") UUID resourceLeaseToken,
                            @Param("leaseSeconds") int leaseSeconds);
    int deleteResourceLease(@Param("resourceLeaseToken") UUID resourceLeaseToken,
                            @Param("jobId") UUID jobId, @Param("jobLeaseToken") UUID jobLeaseToken);
}

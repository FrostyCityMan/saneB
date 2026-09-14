package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.dto.AttachmentBatchRequests;
import com.saneb.domain.announcementattachment.vo.*;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AnnouncementAttachmentBatchDao {
    AttachmentPolicyRow selectPolicyDetails(@Param("policyId") UUID policyId,@Param("lock") boolean lock);
    List<AttachmentBatchRows.Bucket> selectScopeCounts(AttachmentBatchRequests.Scope scope);
    List<AttachmentBatchRows.Candidate> selectCandidateList(AttachmentBatchRequests.Scope scope);
    List<AttachmentBatchRows.Candidate> selectFixedSourceCandidateList(@Param("sourceIds") List<UUID> sourceIds);
    List<UUID> selectSourceLocks(@Param("sourceIds") List<UUID> sourceIds);
    String selectRequestLock(@Param("key") UUID key);
    AttachmentBatchRows.Row selectRequestDetails(@Param("key") UUID key);
    AttachmentBatchRows.Row selectBatchDetails(@Param("batchId") UUID batchId,@Param("lock") boolean lock);
    List<AttachmentBatchRows.Row> selectBatchList(AttachmentBatchRows.Search search);
    long selectBatchCount();
    List<AttachmentBatchRows.Item> selectItemList(AttachmentBatchRows.Search search);
    List<AttachmentBatchRows.Bucket> selectJobCounts(@Param("batchId") UUID batchId);
    int insertBatch(AttachmentBatchRows.Insert command);
    int insertScopeJob(AttachmentBatchRows.ItemInsert command);
    int updateScopeCancellation(@Param("batchId") UUID batchId,@Param("expectedVersion") int expectedVersion);
    int updateScopeJobsCancelled(@Param("batchId") UUID batchId);
    List<AttachmentBatchRows.ExecutionItem> selectExecutionItemList(@Param("batchId") UUID batchId);
    List<AttachmentBatchRows.Candidate> selectFixedCandidateList(@Param("batchId") UUID batchId);
    int updateCollectionStart(@Param("batchId") UUID batchId,@Param("expectedVersion") int expectedVersion,
            @Param("actorId") UUID actorId,@Param("approvalHash") String approvalHash);
    int updateCollectionJobsPending(@Param("batchId") UUID batchId);
    int updateCollectionPause(@Param("batchId") UUID batchId,@Param("expectedVersion") int expectedVersion);
    int updateCollectionResume(@Param("batchId") UUID batchId,@Param("expectedVersion") int expectedVersion);
    int updateCollectionProgress();
}

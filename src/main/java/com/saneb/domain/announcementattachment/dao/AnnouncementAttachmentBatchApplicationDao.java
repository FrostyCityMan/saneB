package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentBatchApplicationRows.*;
import java.util.UUID;
import java.util.List;
import com.saneb.domain.announcementattachment.vo.AttachmentBatchRows;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AnnouncementAttachmentBatchApplicationDao {
    String selectRequestLock(@Param("key") UUID key);
    Action selectRequestDetails(@Param("key") UUID key);
    Action selectActionDetails(@Param("batchId") UUID batchId,@Param("actionId") UUID actionId);
    int insertAction(Action action);
    Counts selectCounts(@Param("batchId") UUID batchId);
    List<Item> selectItemList(AttachmentBatchRows.Search search);
    int updateStart(@Param("batchId") UUID batchId,@Param("version") int version,@Param("actionId") UUID actionId);
    int updateJobsPending(@Param("batchId") UUID batchId,@Param("previewId") UUID previewId);
    int updatePause(@Param("batchId") UUID batchId,@Param("version") int version);
    int updateResume(@Param("batchId") UUID batchId,@Param("version") int version);
    Work selectNextWorkDetails();
    Work selectWorkDetails(@Param("batchId") UUID batchId,@Param("jobId") UUID jobId);
    int updateSourceApplication(@Param("jobId") UUID jobId);
    int updateApplied(@Param("jobId") UUID jobId,@Param("inputHash") String inputHash);
    int updateConflict(@Param("jobId") UUID jobId,@Param("errorCode") String errorCode);
    int updateFailure(@Param("jobId") UUID jobId);
    int updateProgress(@Param("batchId") UUID batchId);
    int updateEmptyProgress();
}

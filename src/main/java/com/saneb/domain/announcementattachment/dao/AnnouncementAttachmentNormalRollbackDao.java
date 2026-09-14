package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentNormalRollbackRows.Action;
import com.saneb.domain.announcementattachment.dto.AttachmentNormalRollbackResponses.JobSummary;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AnnouncementAttachmentNormalRollbackDao {
    UUID selectVisibleSourceDetails(@Param("sourceId") UUID sourceId);
    List<JobSummary> selectJobList(@Param("sourceId") UUID sourceId,@Param("offset") int offset,@Param("size") int size);
    long selectJobCount(@Param("sourceId") UUID sourceId);
    String selectStateJson(@Param("sourceId") UUID sourceId,@Param("jobId") UUID jobId);
    String selectRequestLock(@Param("key") UUID key);
    UUID selectSourceLock(@Param("sourceId") UUID sourceId);
    UUID selectJobLock(@Param("sourceId") UUID sourceId,@Param("jobId") UUID jobId);
    Action selectRequestDetails(@Param("key") UUID key);
    Action selectActionDetails(@Param("sourceId") UUID sourceId,@Param("jobId") UUID jobId,@Param("actionId") UUID actionId);
    int insertAction(Action action);
    int updateSourceRestoration(@Param("actionId") UUID actionId);
    int updateConfirmationCurrent(@Param("actionId") UUID actionId);
    int updateRolledBack(@Param("actionId") UUID actionId);
}

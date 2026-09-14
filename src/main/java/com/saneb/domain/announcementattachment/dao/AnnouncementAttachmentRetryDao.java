package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentRetryFileRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentRetryDao {
    boolean selectRetryControlAllowed();
    boolean selectRetryRateAllowed(@Param("sourceId") UUID sourceId);
    int insertRetryFile(@Param("jobId") UUID jobId,@Param("sourceId") UUID sourceId,@Param("setId") UUID setId,@Param("fileId") UUID fileId);
    List<AttachmentRetryFileRow> selectRetryFileList(@Param("jobId") UUID jobId,@Param("leaseToken") UUID leaseToken);
}

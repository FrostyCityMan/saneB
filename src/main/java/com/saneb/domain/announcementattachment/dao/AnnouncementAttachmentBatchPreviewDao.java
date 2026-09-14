package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentBatchPreviewRows;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AnnouncementAttachmentBatchPreviewDao {
    String selectRequestLock(@Param("key") UUID key);
    AttachmentBatchPreviewRows.Preview selectRequestDetails(@Param("key") UUID key);
    AttachmentBatchPreviewRows.Preview selectPreviewDetails(@Param("batchId") UUID batchId,@Param("previewId") UUID previewId);
    AttachmentBatchPreviewRows.Preview selectCurrentPreviewDetails(@Param("batchId") UUID batchId);
    List<AttachmentBatchPreviewRows.LiveItem> selectLiveItemList(@Param("batchId") UUID batchId);
    AttachmentBatchPreviewRows.LiveItem selectLiveItemDetails(@Param("batchId") UUID batchId,@Param("jobId") UUID jobId);
    List<AttachmentBatchPreviewRows.Item> selectItemList(AttachmentBatchPreviewRows.Search search);
    long selectItemCount(@Param("previewId") UUID previewId);
    int updatePreviewRunning(@Param("batchId") UUID batchId,@Param("expectedVersion") int expectedVersion);
    int insertPreview(AttachmentBatchPreviewRows.Preview preview);
    int insertItem(AttachmentBatchPreviewRows.ItemInsert item);
    int updateJobSelection(@Param("batchId") UUID batchId,@Param("selectedJobIds") List<UUID> selectedJobIds);
    int updatePreviewReady(@Param("batchId") UUID batchId,@Param("expectedVersion") int expectedVersion,
            @Param("statusCode") String statusCode,@Param("previewHash") String previewHash,@Param("previewId") UUID previewId);
}

package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentBackfillSegmentDao {
    String selectRequestLock(@Param("key") UUID key);
    AttachmentBackfillRows.Run selectRunDetails(@Param("runId") UUID runId,@Param("lock") boolean lock);
    AttachmentBackfillRows.Segment selectSegmentDetails(@Param("runId") UUID runId,@Param("segmentNo") long segmentNo,@Param("lock") boolean lock);
    List<AttachmentBackfillRows.Item> selectFixedItemList(@Param("runId") UUID runId,@Param("segmentNo") long segmentNo);
    AttachmentBackfillSegmentRows.Link selectLinkDetails(@Param("runId") UUID runId,@Param("segmentNo") long segmentNo);
    AttachmentBackfillSegmentRows.Link selectRequestDetails(@Param("key") UUID key);
    int insertLink(AttachmentBackfillSegmentRows.Insert row);
    AttachmentBackfillSegmentRows.Totals selectTotals(@Param("runId") UUID runId);
    List<AttachmentBackfillSegmentRows.Outcome> selectOutcomeCounts(@Param("runId") UUID runId);
}

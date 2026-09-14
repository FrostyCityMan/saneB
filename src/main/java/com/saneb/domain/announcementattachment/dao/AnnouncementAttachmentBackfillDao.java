package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.dto.AttachmentBackfillRequests;
import com.saneb.domain.announcementattachment.vo.AttachmentBackfillRows;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentBackfillDao {
    boolean selectRequestLock(@Param("key") UUID key);
    AttachmentBackfillRows.Digest selectScopeDigest(@Param("scope") AttachmentBackfillRequests.Scope scope);
    AttachmentBackfillRows.Run selectRequestDetails(@Param("key") UUID key);
    AttachmentBackfillRows.Run selectRunDetails(@Param("runId") UUID runId);
    List<AttachmentBackfillRows.Run> selectRunList(AttachmentBackfillRows.Search search);
    long selectRunCount();
    AttachmentBackfillRows.Totals selectRunTotals(@Param("runId") UUID runId);
    List<AttachmentBackfillRows.Segment> selectSegmentList(AttachmentBackfillRows.Search search);
    AttachmentBackfillRows.Segment selectSegmentDetails(@Param("runId") UUID runId,@Param("segmentNo") long segmentNo);
    List<AttachmentBackfillRows.Item> selectItemList(AttachmentBackfillRows.Search search);
    int insertRun(AttachmentBackfillRows.Insert row);
    long insertSegments(AttachmentBackfillRows.Materialize command);
    long insertItems(AttachmentBackfillRows.Materialize command);
}

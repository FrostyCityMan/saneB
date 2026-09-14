package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaEvidenceRows.*;
import com.saneb.domain.announcementattachment.vo.AttachmentProviderQaManagementRows.Run;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentProviderQaEvidenceDao {
    List<Run> selectLatestRunList(Scope scope);
    List<Item> selectEvidenceList(Page page);
    long selectEvidenceCount(@Param("runId") UUID runId);
    String selectRequiredScope(@Param("runId") UUID runId);
}

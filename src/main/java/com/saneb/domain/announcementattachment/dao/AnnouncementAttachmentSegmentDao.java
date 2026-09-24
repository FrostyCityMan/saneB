package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentSegmentRows;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentSegmentDao {
    AttachmentSegmentRows.Input selectExtractionDetails(@Param("sourceId") UUID sourceId, @Param("extractionId") UUID extractionId);
    AttachmentSegmentRows.Binding selectEvaluationBindingDetails(@Param("sourceId") UUID sourceId,
            @Param("extractionId") UUID extractionId, @Param("evaluationId") UUID evaluationId);
    AttachmentSegmentRows.Stored selectAnalysisDetails(@Param("sourceId") UUID sourceId, @Param("extractionId") UUID extractionId,
            @Param("analysisVersion") String analysisVersion, @Param("rulesHash") String rulesHash);
    int insertAnalysis(AttachmentSegmentRows.Insert command);
}

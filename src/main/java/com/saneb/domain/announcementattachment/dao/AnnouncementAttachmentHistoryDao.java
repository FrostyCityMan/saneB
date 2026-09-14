package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import com.saneb.domain.announcementattachment.vo.AttachmentHistoryRows;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentHistoryDao {
    long selectEvaluationCount(@Param("sourceId") UUID sourceId);
    List<AttachmentEvaluationRows.Evaluation> selectEvaluationList(@Param("sourceId") UUID sourceId,
            @Param("offset") int offset,@Param("size") int size);
    List<String> selectAutoTargetList(@Param("sourceId") UUID sourceId,@Param("evaluationId") UUID evaluationId);
    List<String> selectAutoSupportList(@Param("sourceId") UUID sourceId,@Param("evaluationId") UUID evaluationId);
    boolean selectInputExists(@Param("sourceId") UUID sourceId,@Param("evaluationId") UUID evaluationId,@Param("fileId") UUID fileId);
    long selectInputCount(@Param("sourceId") UUID sourceId,@Param("evaluationId") UUID evaluationId);
    List<AttachmentHistoryRows.Input> selectInputList(@Param("sourceId") UUID sourceId,@Param("evaluationId") UUID evaluationId,
            @Param("offset") int offset,@Param("size") int size);
    long selectMatchCount(@Param("sourceId") UUID sourceId,@Param("evaluationId") UUID evaluationId,@Param("fileId") UUID fileId);
    List<AttachmentHistoryRows.Match> selectMatchList(@Param("sourceId") UUID sourceId,@Param("evaluationId") UUID evaluationId,
            @Param("fileId") UUID fileId,@Param("offset") int offset,@Param("size") int size);
}

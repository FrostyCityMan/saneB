package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationCommands;
import com.saneb.domain.announcementattachment.vo.AttachmentEvaluationRows;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentEvaluationDao {
    String selectPolicyHash(@Param("policyId") UUID policyId, @Param("releaseId") UUID releaseId);
    AttachmentEvaluationRows.Base selectBaseDetails(@Param("sourceId") UUID sourceId, @Param("evaluationId") UUID evaluationId);
    List<String> selectBaseTargetList(@Param("evaluationId") UUID evaluationId);
    List<String> selectBaseSupportList(@Param("evaluationId") UUID evaluationId);
    List<AttachmentEvaluationRows.File> selectFileInputList(@Param("sourceId") UUID sourceId, @Param("setId") UUID setId);
    AttachmentEvaluationRows.Evaluation selectEvaluationDetails(@Param("sourceId") UUID sourceId, @Param("evaluationId") UUID evaluationId);
    List<AttachmentEvaluationRows.SegmentReview> selectSegmentReviewList(@Param("sourceId") UUID sourceId, @Param("evaluationId") UUID evaluationId);
    AttachmentEvaluationRows.Evaluation selectInputEvaluationDetails(@Param("sourceId") UUID sourceId,
            @Param("inputHash") String inputHash, @Param("engineVersion") String engineVersion);
    AttachmentEvaluationRows.Evaluation selectJobEvaluationDetails(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken);
    int insertEvaluation(AttachmentEvaluationCommands.Evaluation command);
    int insertInput(AttachmentEvaluationCommands.Input command);
    int insertMatch(AttachmentEvaluationCommands.Match command);
    int insertTargetTag(@Param("evaluationId") UUID evaluationId, @Param("code") String code);
    int insertSupportTag(@Param("evaluationId") UUID evaluationId, @Param("code") String code);
    int updatePreviousEvaluationsStale(@Param("sourceId") UUID sourceId);
    int updatePreviousConfirmationsStale(@Param("sourceId") UUID sourceId);
    int updateEvaluationCurrent(@Param("evaluationId") UUID evaluationId, @Param("sourceId") UUID sourceId);
    int updateSourceEvaluation(@Param("sourceId") UUID sourceId, @Param("evaluationId") UUID evaluationId,
                              @Param("sourceVersion") int sourceVersion, @Param("attachmentVersion") int attachmentVersion);
    int updateJobCompleted(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken,
                           @Param("evaluationId") UUID evaluationId, @Param("previewHash") String previewHash,
                           @Param("jobStatus") String jobStatus);
}

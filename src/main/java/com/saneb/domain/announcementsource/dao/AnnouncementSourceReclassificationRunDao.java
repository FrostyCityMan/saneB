package com.saneb.domain.announcementsource.dao;

import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunInsertCommand;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunItemRow;
import com.saneb.domain.announcementsource.vo.AnnouncementSourceReclassificationRunRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementSourceReclassificationRunDao {

    int insertRun(AnnouncementSourceReclassificationRunInsertCommand command);

    int insertRunTargetItems(@Param("runId") UUID runId);

    int updateRunTotalCount(@Param("runId") UUID runId);

    AnnouncementSourceReclassificationRunRow selectRunDetails(@Param("runId") UUID runId);

    List<AnnouncementSourceReclassificationRunRow> selectRunList(@Param("limit") int limit);

    AnnouncementSourceReclassificationRunRow selectNextRunnableRunDetails();

    List<AnnouncementSourceReclassificationRunItemRow> selectRunItemList(
            @Param("runId") UUID runId,
            @Param("itemStatusCode") String itemStatusCode,
            @Param("limit") int limit
    );

    int selectRunItemStatusCount(
            @Param("runId") UUID runId,
            @Param("itemStatusCodes") List<String> itemStatusCodes
    );

    int updateRunStatus(
            @Param("runId") UUID runId,
            @Param("expectedVersion") int expectedVersion,
            @Param("expectedStatusCodes") List<String> expectedStatusCodes,
            @Param("nextStatusCode") String nextStatusCode,
            @Param("actionReasonHash") String actionReasonHash
    );

    int updateItemPreviewed(
            @Param("itemId") UUID itemId,
            @Param("predictedSemanticStatusCode") String predictedSemanticStatusCode,
            @Param("predictedReasonCode") String predictedReasonCode,
            @Param("predictionHash") String predictionHash
    );

    int updateItemApplied(
            @Param("itemId") UUID itemId,
            @Param("evaluationId") UUID evaluationId,
            @Param("appliedClassificationVersion") int appliedClassificationVersion
    );

    int updateItemRolledBack(@Param("itemId") UUID itemId);

    int updateItemFailure(
            @Param("itemId") UUID itemId,
            @Param("expectedStatusCode") String expectedStatusCode,
            @Param("nextStatusCode") String nextStatusCode,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    int updateAppliedEvaluationNotCurrent(
            @Param("sourceId") UUID sourceId,
            @Param("evaluationId") UUID evaluationId
    );

    int updatePreviousEvaluationCurrent(
            @Param("sourceId") UUID sourceId,
            @Param("evaluationId") UUID evaluationId
    );

    int updateConfirmedTargetCurrentForEvaluation(
            @Param("sourceId") UUID sourceId,
            @Param("evaluationId") UUID evaluationId
    );

    int updateConfirmedSupportCurrentForEvaluation(
            @Param("sourceId") UUID sourceId,
            @Param("evaluationId") UUID evaluationId
    );

    int updateSnapshotProjectionRollback(
            @Param("sourceId") UUID sourceId,
            @Param("semanticStatusCode") String semanticStatusCode,
            @Param("semanticReasonCode") String semanticReasonCode,
            @Param("semanticMatchedKeywords") String semanticMatchedKeywords,
            @Param("reviewStatusCode") String reviewStatusCode,
            @Param("expectedVersion") int expectedVersion
    );
}

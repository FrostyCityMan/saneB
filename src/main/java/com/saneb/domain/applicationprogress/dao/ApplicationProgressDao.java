package com.saneb.domain.applicationprogress.dao;

import com.saneb.domain.applicationprogress.vo.ApplicationActionLogCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationChecklistRow;
import com.saneb.domain.applicationprogress.vo.ApplicationChecklistSaveCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationProgressCreateCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationProgressRow;
import com.saneb.domain.applicationprogress.vo.ApplicationProgressSearchCondition;
import com.saneb.domain.applicationprogress.vo.ApplicationStepStateCreateCommand;
import com.saneb.domain.applicationprogress.vo.ApplicationStepStateRow;
import com.saneb.domain.applicationprogress.vo.AnnouncementProgressStepRow;
import com.saneb.domain.applicationprogress.vo.AuditLogCommand;
import com.saneb.domain.applicationprogress.vo.MatchingCaseProgressRow;
import com.saneb.domain.applicationprogress.vo.ProgressReceiptCommand;
import com.saneb.domain.applicationprogress.vo.ProgressResultCommand;
import com.saneb.domain.applicationprogress.vo.StepButtonRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApplicationProgressDao {

    List<ApplicationProgressRow> selectApplicationProgressList(ApplicationProgressSearchCondition condition);

    long selectApplicationProgressCount(ApplicationProgressSearchCondition condition);

    ApplicationProgressRow selectApplicationProgressDetails(@Param("progressId") UUID progressId);

    ApplicationProgressRow selectApplicationProgressByMatchingCaseId(@Param("matchingCaseId") UUID matchingCaseId);

    MatchingCaseProgressRow selectMatchingCaseForProgress(@Param("matchingCaseId") UUID matchingCaseId);

    List<AnnouncementProgressStepRow> selectActiveAnnouncementProgressStepList(@Param("announcementId") UUID announcementId);

    List<ApplicationStepStateRow> selectApplicationStepStateList(@Param("progressId") UUID progressId);

    ApplicationStepStateRow selectApplicationStepState(
            @Param("progressId") UUID progressId,
            @Param("stepId") UUID stepId
    );

    List<ApplicationChecklistRow> selectApplicationChecklistList(@Param("progressId") UUID progressId);

    StepButtonRow selectStepButton(
            @Param("stepId") UUID stepId,
            @Param("buttonCode") String buttonCode
    );

    UUID selectNextActiveStepId(
            @Param("announcementId") UUID announcementId,
            @Param("stepOrder") int stepOrder
    );

    long selectRequiredUncheckedDocumentCount(
            @Param("progressId") UUID progressId,
            @Param("stepId") UUID stepId
    );

    long selectStepDocumentBelongsToStepCount(
            @Param("stepId") UUID stepId,
            @Param("stepDocumentId") UUID stepDocumentId
    );

    void insertApplicationProgress(ApplicationProgressCreateCommand command);

    void insertApplicationStepState(ApplicationStepStateCreateCommand command);

    int updateMatchingCaseStatusToProgressed(
            @Param("matchingCaseId") UUID matchingCaseId,
            @Param("actorUserId") UUID actorUserId
    );

    int updateApplicationStepStateStatus(
            @Param("progressId") UUID progressId,
            @Param("stepId") UUID stepId,
            @Param("statusCode") String statusCode,
            @Param("actorUserId") UUID actorUserId
    );

    int updateApplicationProgressCurrentStep(
            @Param("progressId") UUID progressId,
            @Param("currentStepId") UUID currentStepId,
            @Param("statusCode") String statusCode,
            @Param("actorUserId") UUID actorUserId
    );

    void insertApplicationActionLog(ApplicationActionLogCommand command);

    void saveApplicationChecklist(ApplicationChecklistSaveCommand command);

    int updateApplicationProgressReceipt(ProgressReceiptCommand command);

    int updateApplicationProgressResult(ProgressResultCommand command);

    void insertAuditLog(AuditLogCommand command);
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApplicationProgressDao.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<ApplicationProgressRow> selectApplicationProgressList(ApplicationProgressSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectApplicationProgressCount(ApplicationProgressSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressRow selectApplicationProgressDetails(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressRow selectApplicationProgressByMatchingCaseId(@Param("matchingCaseId") UUID matchingCaseId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @return 처리 결과
     */
    MatchingCaseProgressRow selectMatchingCaseForProgress(@Param("matchingCaseId") UUID matchingCaseId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementProgressStepRow> selectActiveAnnouncementProgressStepList(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    List<ApplicationStepStateRow> selectApplicationStepStateList(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationStepStateRow selectApplicationStepState(
            @Param("progressId") UUID progressId,
            @Param("stepId") UUID stepId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    List<ApplicationChecklistRow> selectApplicationChecklistList(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    List<StepButtonRow> selectStepButtonList(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param stepId 입력 값
     *
     * @param buttonCode 입력 값
     *
     * @return 처리 결과
     */
    StepButtonRow selectStepButton(
            @Param("stepId") UUID stepId,
            @Param("buttonCode") String buttonCode
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @param stepOrder 입력 값
     *
     * @return 처리 결과
     */
    UUID selectNextActiveStepId(
            @Param("announcementId") UUID announcementId,
            @Param("stepOrder") int stepOrder
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @return 처리 결과
     */
    long selectRequiredUncheckedDocumentCount(
            @Param("progressId") UUID progressId,
            @Param("stepId") UUID stepId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param stepId 입력 값
     *
     * @param stepDocumentId 입력 값
     *
     * @return 처리 결과
     */
    long selectStepDocumentBelongsToStepCount(
            @Param("stepId") UUID stepId,
            @Param("stepDocumentId") UUID stepDocumentId
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertApplicationProgress(ApplicationProgressCreateCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertApplicationStepState(ApplicationStepStateCreateCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param matchingCaseId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
    int updateMatchingCaseStatusToProgressed(
            @Param("matchingCaseId") UUID matchingCaseId,
            @Param("actorUserId") UUID actorUserId
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param progressId 입력 값
     *
     * @param stepId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
    int updateApplicationStepStateStatus(
            @Param("progressId") UUID progressId,
            @Param("stepId") UUID stepId,
            @Param("statusCode") String statusCode,
            @Param("actorUserId") UUID actorUserId
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param progressId 입력 값
     *
     * @param currentStepId 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
    int updateApplicationProgressCurrentStep(
            @Param("progressId") UUID progressId,
            @Param("currentStepId") UUID currentStepId,
            @Param("statusCode") String statusCode,
            @Param("actorUserId") UUID actorUserId
    );

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param progressId 입력 값
     *
     * @param actorUserId 입력 값
     *
     * @return 처리 결과
     */
    int touchApplicationProgress(
            @Param("progressId") UUID progressId,
            @Param("actorUserId") UUID actorUserId
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertApplicationActionLog(ApplicationActionLogCommand command);

    /**
     * 업무 데이터를 저장합니다.
     *
     * @param command 입력 값
     */
    void saveApplicationChecklist(ApplicationChecklistSaveCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateApplicationProgressReceipt(ProgressReceiptCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateApplicationProgressResult(ProgressResultCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}

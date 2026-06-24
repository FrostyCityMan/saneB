/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AnnouncementDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.announcement.dao;

import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementDocumentRequirementCommand;
import com.saneb.domain.announcement.vo.AnnouncementDocumentRequirementRow;
import com.saneb.domain.announcement.vo.AnnouncementApprovalDecisionCommand;
import com.saneb.domain.announcement.vo.AnnouncementApprovalRequestCommand;
import com.saneb.domain.announcement.vo.AnnouncementApprovalStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementIndustryConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementIndustryConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementManualStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementNumericConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementNumericConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementOptionCommand;
import com.saneb.domain.announcement.vo.AnnouncementOptionConditionCommand;
import com.saneb.domain.announcement.vo.AnnouncementOptionConditionRow;
import com.saneb.domain.announcement.vo.AnnouncementOptionRow;
import com.saneb.domain.announcement.vo.AnnouncementProgressStepCommand;
import com.saneb.domain.announcement.vo.AnnouncementProgressStepRow;
import com.saneb.domain.announcement.vo.AnnouncementSaveCommand;
import com.saneb.domain.announcement.vo.AnnouncementSearchCondition;
import com.saneb.domain.announcement.vo.AnnouncementStatusHistoryCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepButtonCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepButtonRow;
import com.saneb.domain.announcement.vo.AnnouncementStepDocumentCommand;
import com.saneb.domain.announcement.vo.AnnouncementStepDocumentRow;
import com.saneb.domain.announcement.vo.AnnouncementStandardDocumentFieldRow;
import com.saneb.domain.announcement.vo.AnnouncementSummaryRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementSummaryRow> selectAnnouncementList(AnnouncementSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectAnnouncementCount(AnnouncementSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementDetailsRow selectAnnouncementDetails(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementOptionRow> selectAnnouncementOptionList(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementIndustryConditionRow> selectAnnouncementIndustryConditionList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementNumericConditionRow> selectAnnouncementNumericConditionList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementOptionConditionRow> selectAnnouncementOptionConditionList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementDocumentRequirementRow> selectAnnouncementDocumentRequirementList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementProgressStepRow> selectAnnouncementProgressStepList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementStepDocumentRow> selectAnnouncementStepDocumentList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementStepButtonRow> selectAnnouncementStepButtonList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncement(AnnouncementSaveCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateAnnouncement(AnnouncementSaveCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateAnnouncementManualStatus(AnnouncementManualStatusCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementStatusHistory(AnnouncementStatusHistoryCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    long selectRequestedApprovalRequestCount(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementApprovalRequest(AnnouncementApprovalRequestCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateAnnouncementApprovalDecision(AnnouncementApprovalDecisionCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateAnnouncementApprovalStatus(AnnouncementApprovalStatusCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementOptions(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementOption(AnnouncementOptionCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementIndustryConditions(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementIndustryCondition(AnnouncementIndustryConditionCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementNumericConditions(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementNumericCondition(AnnouncementNumericConditionCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementOptionConditions(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementOptionCondition(AnnouncementOptionConditionCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementDocumentRequirements(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementDocumentRequirement(AnnouncementDocumentRequirementCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param standardFieldId 입력 값
     *
     * @return 처리 결과
     */
    AnnouncementStandardDocumentFieldRow selectStandardDocumentFieldDetails(
            @Param("standardFieldId") UUID standardFieldId
    );

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementStepButtons(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementStepDocuments(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param announcementId 입력 값
     */
    void deleteAnnouncementProgressSteps(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementProgressStep(AnnouncementProgressStepCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementStepButton(AnnouncementStepButtonCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementStepDocument(AnnouncementStepDocumentCommand command);
}

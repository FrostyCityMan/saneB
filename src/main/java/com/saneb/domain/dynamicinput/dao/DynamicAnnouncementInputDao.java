/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DynamicAnnouncementInputDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.dynamicinput.dao;

import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputOptionRow;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementCommand;
import com.saneb.domain.dynamicinput.vo.AnnouncementInputRequirementRow;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueCommand;
import com.saneb.domain.dynamicinput.vo.ApplicationInputValueRow;
import com.saneb.domain.dynamicinput.vo.ApplicationProgressInputRow;
import com.saneb.domain.dynamicinput.vo.AuditLogCommand;
import com.saneb.domain.dynamicinput.vo.StandardDocumentFieldRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DynamicAnnouncementInputDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    long selectAnnouncementCount(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementInputRequirementRow> selectAnnouncementInputRequirementList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementInputOptionRow> selectAnnouncementInputOptionList(
            @Param("announcementId") UUID announcementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requirementId 입력 값
     *
     * @return 처리 결과
     */
    List<AnnouncementInputOptionRow> selectAnnouncementInputOptionListByRequirementId(
            @Param("requirementId") UUID requirementId
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param announcementId 입력 값
     *
     * @return 처리 결과
     */
    long selectApplicationProgressCountByAnnouncementId(@Param("announcementId") UUID announcementId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requirementId 입력 값
     *
     * @return 처리 결과
     */
    long selectApplicationInputValueCountByRequirementId(@Param("requirementId") UUID requirementId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requirementId 입력 값
     *
     * @param optionCode 입력 값
     *
     * @return 처리 결과
     */
    long selectApplicationInputValueCountByRequirementOption(
            @Param("requirementId") UUID requirementId,
            @Param("optionCode") String optionCode
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementInputRequirement(AnnouncementInputRequirementCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateAnnouncementInputRequirement(AnnouncementInputRequirementCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param requirementId 입력 값
     */
    void deleteAnnouncementInputOptionsByRequirementId(@Param("requirementId") UUID requirementId);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param requirementId 입력 값
     *
     * @param optionCode 입력 값
     */
    void deleteAnnouncementInputOption(
            @Param("requirementId") UUID requirementId,
            @Param("optionCode") String optionCode
    );

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param requirementId 입력 값
     */
    void deleteAnnouncementInputRequirement(@Param("requirementId") UUID requirementId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAnnouncementInputOption(AnnouncementInputOptionCommand command);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateAnnouncementInputOption(AnnouncementInputOptionCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    ApplicationProgressInputRow selectApplicationProgressForInput(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    List<ApplicationInputValueRow> selectApplicationInputValueList(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param progressId 입력 값
     */
    void deleteApplicationInputValues(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertApplicationInputValue(ApplicationInputValueCommand command);

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
     * 업무 데이터를 조회합니다.
     *
     * @param progressId 입력 값
     *
     * @return 처리 결과
     */
    long selectMissingRequiredApplicationInputCount(@Param("progressId") UUID progressId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param documentTypeCode 입력 값
     *
     * @param scopeCode 입력 값
     *
     * @return 처리 결과
     */
    List<StandardDocumentFieldRow> selectStandardDocumentFieldList(
            @Param("documentTypeCode") String documentTypeCode,
            @Param("scopeCode") String scopeCode
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param standardFieldId 입력 값
     *
     * @return 처리 결과
     */
    StandardDocumentFieldRow selectStandardDocumentFieldDetails(@Param("standardFieldId") UUID standardFieldId);
}

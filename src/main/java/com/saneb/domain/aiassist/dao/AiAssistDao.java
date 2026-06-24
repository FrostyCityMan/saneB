/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AiAssistDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.aiassist.dao;

import com.saneb.domain.aiassist.vo.AiAssistInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistResultInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistRow;
import com.saneb.domain.aiassist.vo.AiAssistSearchCondition;
import com.saneb.domain.aiassist.vo.AuditLogCommand;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAssistDao {

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAiAssistRequest(AiAssistInsertCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAiAssistResult(AiAssistResultInsertCommand command);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<AiAssistRow> selectAiAssistList(AiAssistSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectAiAssistCount(AiAssistSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param requestId 입력 값
     *
     * @return 처리 결과
     */
    AiAssistRow selectAiAssistDetails(@Param("requestId") UUID requestId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param resultId 입력 값
     *
     * @return 처리 결과
     */
    AiAssistRow selectAiAssistDetailsByResultId(@Param("resultId") UUID resultId);

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param resultId 입력 값
     *
     * @param reviewStatusCode 입력 값
     *
     * @param actorUserId 입력 값
     */
    void updateAiAssistResultReviewStatus(
            @Param("resultId") UUID resultId,
            @Param("reviewStatusCode") String reviewStatusCode,
            @Param("actorUserId") UUID actorUserId
    );

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}

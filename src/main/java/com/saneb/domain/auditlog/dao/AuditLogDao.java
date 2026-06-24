/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuditLogDao.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auditlog.dao;

import com.saneb.domain.auditlog.vo.AuditLogDetailsRow;
import com.saneb.domain.auditlog.vo.AuditLogSearchCondition;
import com.saneb.domain.auditlog.vo.AuditLogSummaryRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogDao {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<AuditLogSummaryRow> selectAuditLogList(AuditLogSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectAuditLogCount(AuditLogSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param auditLogId 입력 값
     *
     * @return 처리 결과
     */
    AuditLogDetailsRow selectAuditLogDetails(@Param("auditLogId") UUID auditLogId);
}

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuditLogService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auditlog.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auditlog.dto.AuditLogDetailsResponse;
import com.saneb.domain.auditlog.dto.AuditLogSummaryResponse;
import java.util.UUID;

public interface AuditLogService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param actionCode 입력 값
     *
     * @param resourceType 입력 값
     *
     * @param resultCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<AuditLogSummaryResponse> selectAuditLogList(
            String keyword,
            String actionCode,
            String resourceType,
            String resultCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param auditLogId 입력 값
     *
     * @return 처리 결과
     */
    AuditLogDetailsResponse selectAuditLogDetails(UUID auditLogId);
}

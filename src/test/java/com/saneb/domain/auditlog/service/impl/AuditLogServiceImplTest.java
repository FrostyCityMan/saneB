/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuditLogServiceImplTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auditlog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.domain.auditlog.dao.AuditLogDao;
import com.saneb.domain.auditlog.dto.AuditLogDetailsResponse;
import com.saneb.domain.auditlog.dto.AuditLogSummaryResponse;
import com.saneb.domain.auditlog.vo.AuditLogDetailsRow;
import com.saneb.domain.auditlog.vo.AuditLogSearchCondition;
import com.saneb.domain.auditlog.vo.AuditLogSummaryRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    private static final UUID AUDIT_LOG_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Mock
    private AuditLogDao auditLogDao;

    private AuditLogServiceImpl auditLogService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogDao);
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectAuditLogListMapsLabelsAndPagination() {
        when(auditLogDao.selectAuditLogCount(org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        when(auditLogDao.selectAuditLogList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(summaryRow()));

        var response = auditLogService.selectAuditLogList(" admin01 ", null, "user", "success", 1, 20);

        AuditLogSummaryResponse item = response.items().getFirst();
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(item.actorDisplayName()).isEqualTo("관리자 (admin01)");
        assertThat(item.actionLabel()).isEqualTo("권한 변경");
        assertThat(item.resourceLabel()).isEqualTo("회원");
        assertThat(item.resultLabel()).isEqualTo("성공");

        ArgumentCaptor<AuditLogSearchCondition> captor = ArgumentCaptor.forClass(AuditLogSearchCondition.class);
        org.mockito.Mockito.verify(auditLogDao).selectAuditLogCount(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("admin01");
        assertThat(captor.getValue().resourceType()).isEqualTo("USER");
        assertThat(captor.getValue().resultCode()).isEqualTo("SUCCESS");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectAuditLogDetailsMapsMetadata() {
        when(auditLogDao.selectAuditLogDetails(AUDIT_LOG_ID)).thenReturn(detailsRow());

        AuditLogDetailsResponse response = auditLogService.selectAuditLogDetails(AUDIT_LOG_ID);

        assertThat(response.metadataJson()).isEqualTo("{\"changedCount\":\"1\"}");
        assertThat(response.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(response.actionLabel()).isEqualTo("권한 변경");
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectAuditLogDetailsThrowsWhenMissing() {
        assertThatThrownBy(() -> auditLogService.selectAuditLogDetails(AUDIT_LOG_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 업무 데이터를 조회합니다.
     */
    @Test
    void selectAuditLogListRejectsInvalidResultCode() {
        assertThatThrownBy(() -> auditLogService.selectAuditLogList(null, null, null, "BAD", 1, 20))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuditLogSummaryRow summaryRow() {
        return new AuditLogSummaryRow(
                AUDIT_LOG_ID,
                USER_ID,
                "admin01",
                "관리자",
                "USER_ROLES_UPDATE",
                "USER",
                USER_ID,
                "SUCCESS",
                CREATED_AT
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuditLogDetailsRow detailsRow() {
        return new AuditLogDetailsRow(
                AUDIT_LOG_ID,
                USER_ID,
                "admin01",
                "관리자",
                "USER_ROLES_UPDATE",
                "USER",
                USER_ID,
                "SUCCESS",
                "127.0.0.1",
                "JUnit",
                "{\"changedCount\":\"1\"}",
                CREATED_AT
        );
    }
}

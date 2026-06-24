/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AuditLogControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.auditlog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auditlog.dto.AuditLogDetailsResponse;
import com.saneb.domain.auditlog.dto.AuditLogSummaryResponse;
import com.saneb.domain.auditlog.service.AuditLogService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AuditLogControllerSmokeTest {

    static final UUID AUDIT_LOG_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");
    static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(auditLogService.selectAuditLogList(any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(sampleSummary()), 1, 20, 1));
        when(auditLogService.selectAuditLogDetails(AUDIT_LOG_ID)).thenReturn(sampleDetails());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAuditLogListReturnsPagedApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].auditLogId").value(AUDIT_LOG_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].actorDisplayName").value("관리자 (admin01)"))
                .andExpect(jsonPath("$.data.items[0].actionLabel").value("권한 변경"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "approver01", roles = "APPROVER")
    void selectAuditLogDetailsAllowsApproverUser() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs/{auditLogId}", AUDIT_LOG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadataJson").value("{\"changedCount\":\"1\"}"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "user01", roles = "USER")
    void auditLogApisRejectUser() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    static AuditLogSummaryResponse sampleSummary() {
        return new AuditLogSummaryResponse(
                AUDIT_LOG_ID,
                USER_ID,
                "관리자 (admin01)",
                "USER_ROLES_UPDATE",
                "권한 변경",
                "USER",
                "회원",
                USER_ID,
                "SUCCESS",
                "성공",
                OffsetDateTime.parse("2026-06-08T10:00:00+09:00")
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    static AuditLogDetailsResponse sampleDetails() {
        return new AuditLogDetailsResponse(
                AUDIT_LOG_ID,
                USER_ID,
                "관리자 (admin01)",
                "USER_ROLES_UPDATE",
                "권한 변경",
                "USER",
                "회원",
                USER_ID,
                "SUCCESS",
                "성공",
                "127.0.0.1",
                "JUnit",
                "{\"changedCount\":\"1\"}",
                OffsetDateTime.parse("2026-06-08T10:00:00+09:00")
        );
    }
}

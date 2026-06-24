/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AiAssistControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.aiassist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.aiassist.dto.AiAssistResponse;
import com.saneb.domain.aiassist.service.AiAssistService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AiAssistControllerSmokeTest {

    private static final UUID OPERATOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("93000000-0000-0000-0000-000000000001");
    private static final UUID RESULT_ID = UUID.fromString("93000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiAssistService aiAssistService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        AiAssistResponse response = response("PENDING_REVIEW");
        when(aiAssistService.insertAiAssistRequest(any(), any())).thenReturn(response);
        when(aiAssistService.selectAiAssistRequestList(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(PageResponse.of(List.of(response), 1, 20, 1));
        when(aiAssistService.selectAiAssistRequestDetails(any(), eq(REQUEST_ID))).thenReturn(response);
        when(aiAssistService.updateAiAssistResultReview(any(), eq(RESULT_ID), any())).thenReturn(response("ACCEPTED"));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void insertAiAssistRequestReturnsDraftResponse() throws Exception {
        mockMvc.perform(post("/api/v1/ai-assist/requests")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assistTypeCode": "ANNOUNCEMENT_SUMMARY",
                                  "resourceType": "ANNOUNCEMENT",
                                  "inputText": "공고 원문은 저장하지 않고 해시만 저장합니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.reviewStatusCode").value("PENDING_REVIEW"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectAiAssistRequestListReturnsPageResponse() throws Exception {
        mockMvc.perform(get("/api/v1/ai-assist/requests")
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    /**
     * 업무 데이터를 수정합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void updateAiAssistResultReviewReturnsUpdatedStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/ai-assist/results/{resultId}/review", RESULT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewStatusCode": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatusCode").value("ACCEPTED"));
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void insertAiAssistRequestRejectsUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/ai-assist/requests")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assistTypeCode": "ANNOUNCEMENT_SUMMARY",
                                  "inputText": "권한 없는 요청"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param reviewStatusCode 입력 값
     *
     * @return 처리 결과
     */
    private AiAssistResponse response(String reviewStatusCode) {
        return new AiAssistResponse(
                REQUEST_ID,
                RESULT_ID,
                "ANNOUNCEMENT_SUMMARY",
                "ANNOUNCEMENT",
                null,
                "COMPLETED",
                "LOCAL_SAFE",
                "RULE_TEMPLATE_V1",
                reviewStatusCode,
                "공고 요약 초안",
                OPERATOR_ID,
                CREATED_AT,
                CREATED_AT
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails operatorPrincipal() {
        return principal(List.of("OPERATOR"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails userPrincipal() {
        return principal(List.of("USER"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param roles 입력 값
     *
     * @return 처리 결과
     */
    private AuthenticatedUserDetails principal(List<String> roles) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        OPERATOR_ID,
                        "operator",
                        "password-hash",
                        "Operator User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                roles
        );
    }
}

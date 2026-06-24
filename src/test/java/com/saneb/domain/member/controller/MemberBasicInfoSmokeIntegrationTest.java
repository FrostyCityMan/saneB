/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberBasicInfoSmokeIntegrationTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@EnabledIfEnvironmentVariable(named = "SANEB_MEMBER_BASIC_INFO_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class MemberBasicInfoSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void localUserSelectsAndSavesDocumentInputValues() throws Exception {
        MockHttpSession session = loginLocalUser();
        UUID standardFieldId = selectStandardFieldId("BUSINESS_REGISTRATION", "WORKPLACE_ADDRESS");

        mockMvc.perform(get("/api/v1/member/basic-info").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentInputs[0].documentTypeCode").value("BUSINESS_REGISTRATION"))
                .andExpect(jsonPath("$.data.documentInputs[0].fields[0].fieldLabel").exists())
                .andExpect(jsonPath("$.data.documentInputs[0].fields[0].sortOrder").exists());

        mockMvc.perform(put("/api/v1/member/basic-info")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 1988,
                                  "regionCode": "SEOUL",
                                  "incomePresenceCode": "UNKNOWN",
                                  "families": [],
                                  "documentInputs": [
                                    {
                                      "documentTypeCode": "BUSINESS_REGISTRATION",
                                      "fields": [
                                        {
                                          "standardFieldId": "%s",
                                          "valueText": "서울특별시 중구"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(standardFieldId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentInputs[0].selected").value(true));

        assertThat(selectDocumentInputValueCount("local_user", standardFieldId)).isEqualTo(1);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    private MockHttpSession loginLocalUser() throws Exception {
        for (String password : List.of("password", "new-password")) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "loginId": "local_user",
                                      "password": "%s"
                                    }
                                    """.formatted(password)))
                    .andReturn();
            if (result.getResponse().getStatus() == 200 && result.getRequest().getSession(false) instanceof MockHttpSession session) {
                return session;
            }
        }
        throw new IllegalStateException("local_user login failed for member basic info smoke.");
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param documentTypeCode 입력 값
     *
     * @param fieldKey 입력 값
     *
     * @return 처리 결과
     */
    private UUID selectStandardFieldId(String documentTypeCode, String fieldKey) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM standard_document_fields
                        WHERE document_type_code = ?
                          AND field_key = ?
                        """,
                UUID.class,
                documentTypeCode,
                fieldKey
        );
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param loginId 입력 값
     *
     * @param standardFieldId 입력 값
     *
     * @return 처리 결과
     */
    private long selectDocumentInputValueCount(String loginId, UUID standardFieldId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM member_document_input_values mdiv
                        INNER JOIN users u ON u.id = mdiv.user_id
                        WHERE u.login_id = ?
                          AND mdiv.standard_field_id = ?
                          AND mdiv.value_text = '서울특별시 중구'
                        """,
                Long.class,
                loginId,
                standardFieldId
        );
        return count == null ? 0 : count;
    }
}

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

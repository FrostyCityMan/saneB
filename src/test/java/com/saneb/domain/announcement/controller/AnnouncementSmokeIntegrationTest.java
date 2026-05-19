package com.saneb.domain.announcement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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

@EnabledIfEnvironmentVariable(named = "SANEB_ANNOUNCEMENT_SMOKE", matches = "true")
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class AnnouncementSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void localSeedUserCompletesAnnouncementInputFlow() throws Exception {
        MockHttpSession session = loginLocalOperator();
        String uniqueTitle = "Gate Announcement " + UUID.randomUUID();

        MvcResult createResult = mockMvc.perform(post("/api/v1/announcements")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(uniqueTitle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(uniqueTitle))
                .andExpect(jsonPath("$.data.manualStatusCode").value("NORMAL"))
                .andExpect(jsonPath("$.data.approvalStatusCode").value("DRAFT"))
                .andExpect(jsonPath("$.data.options[0].optionCode").value("ONLINE"))
                .andReturn();
        UUID announcementId = selectAnnouncementId(createResult);

        mockMvc.perform(get("/api/v1/announcements")
                        .session(session)
                        .param("keyword", uniqueTitle)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].announcementId").value(announcementId.toString()));

        mockMvc.perform(put("/api/v1/announcements/{announcementId}", announcementId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(uniqueTitle + " Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(uniqueTitle + " Updated"));

        mockMvc.perform(put("/api/v1/announcements/{announcementId}/conditions", announcementId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conditionsRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(put("/api/v1/announcements/{announcementId}/steps", announcementId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepsRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(patch("/api/v1/announcements/{announcementId}/manual-status", announcementId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manualStatusCode": "PAUSED",
                                  "reason": "Gate smoke"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/announcements/{announcementId}", announcementId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.manualStatusCode").value("PAUSED"))
                .andExpect(jsonPath("$.data.conditions.numericConditions[0].conditionKey").value("ANNUAL_REVENUE"))
                .andExpect(jsonPath("$.data.steps[0].stepName").value("Guide Sent"))
                .andExpect(jsonPath("$.data.steps[0].buttons[0].buttonCode").value("WANTS_TO_PROGRESS"));

        assertThat(selectCount("announcement_numeric_conditions", announcementId)).isEqualTo(1);
        assertThat(selectCount("announcement_progress_steps", announcementId)).isEqualTo(1);
        assertThat(selectStatusHistoryCount(announcementId)).isEqualTo(1);
        assertThat(selectCount("matching_cases", announcementId)).isEqualTo(0);
    }

    private MockHttpSession loginLocalOperator() throws Exception {
        for (String password : List.of("password", "new-password")) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "loginId": "local_operator",
                                      "password": "%s"
                                    }
                                    """.formatted(password)))
                    .andReturn();
            if (result.getResponse().getStatus() == 200 && result.getRequest().getSession(false) instanceof MockHttpSession session) {
                return session;
            }
        }
        throw new IllegalStateException("local_operator login failed for announcement smoke.");
    }

    private UUID selectAnnouncementId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return UUID.fromString(root.path("data").path("announcementId").asText());
    }

    private long selectCount(String tableName, UUID announcementId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(1) FROM " + tableName + " WHERE announcement_id = ?",
                Long.class,
                announcementId
        );
        return count == null ? 0 : count;
    }

    private long selectStatusHistoryCount(UUID announcementId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT count(1)
                        FROM announcement_status_histories
                        WHERE announcement_id = ?
                          AND after_status_code = 'PAUSED'
                        """,
                Long.class,
                announcementId
        );
        return count == null ? 0 : count;
    }

    private String saveRequest(String title) {
        return """
                {
                  "targetTypeCode": "BUSINESS",
                  "title": "%s",
                  "agencyName": "Gate Agency",
                  "summary": "MVP operation test announcement",
                  "applicationStartDate": "2026-06-01",
                  "applicationEndDate": "2026-06-30",
                  "incomeJudgementCode": "VAT_TAX_BASE_ONLY",
                  "minAmount": 1000000,
                  "maxAmount": 5000000,
                  "options": [
                    {
                      "optionGroupCode": "APPLICATION_METHOD",
                      "optionCode": "ONLINE"
                    }
                  ]
                }
                """.formatted(title);
    }

    private String conditionsRequest() {
        return """
                {
                  "industryConditions": [
                    {
                      "conditionTypeCode": "INCLUDE",
                      "ksicCode": "47911"
                    }
                  ],
                  "numericConditions": [
                    {
                      "conditionScopeCode": "BUSINESS",
                      "conditionKey": "ANNUAL_REVENUE",
                      "comparatorCode": "LTE",
                      "valueNumber": 300000000,
                      "unitCode": "KRW"
                    }
                  ],
                  "optionConditions": [
                    {
                      "conditionScopeCode": "BUSINESS",
                      "conditionKey": "BUSINESS_TYPE",
                      "optionCode": "SOLE_PROPRIETOR"
                    }
                  ],
                  "documentRequirements": [
                    {
                      "documentTypeCode": "BUSINESS_REGISTRATION",
                      "required": true,
                      "sortOrder": 1
                    }
                  ]
                }
                """;
    }

    private String stepsRequest() {
        return """
                {
                  "steps": [
                    {
                      "stepOrder": 1,
                      "stepName": "Guide Sent",
                      "guideMessage": "Check the guide.",
                      "actionGuide": "Select next action.",
                      "completionConditionCode": "BUTTON_CLICK",
                      "nextConditionCode": "WANTS_TO_PROGRESS",
                      "active": true,
                      "buttons": [
                        {
                          "buttonCode": "WANTS_TO_PROGRESS",
                          "buttonLabel": "Proceed",
                          "buttonActionCode": "MOVE_NEXT",
                          "sortOrder": 1
                        }
                      ],
                      "documents": [
                        {
                          "documentTypeCode": "BUSINESS_REGISTRATION",
                          "required": true,
                          "sortOrder": 1
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}

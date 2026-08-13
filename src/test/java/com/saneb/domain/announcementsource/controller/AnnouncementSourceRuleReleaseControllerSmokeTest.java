package com.saneb.domain.announcementsource.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceRuleGoldenSetRunResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceRuleReleaseService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementSourceRuleReleaseControllerSmokeTest {

    private static final UUID RELEASE_ID = UUID.fromString("72000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementSourceRuleReleaseService ruleReleaseService;

    @Test
    void selectRuleReleaseListAllowsOperatorAndKeepsPageWrapper() throws Exception {
        when(ruleReleaseService.selectRuleReleaseList(null, 1, 20))
                .thenReturn(PageResponse.of(List.of(), 1, 20, 0));

        mockMvc.perform(get("/api/v1/admin/announcement-source-rule-releases")
                        .with(user(principal("OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void insertRuleReleaseDraftRejectsOperator() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcement-source-rule-releases")
                        .with(user(principal("OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 1,
                                  "changeReason": "정책 보정"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishRuleReleaseRequiresServerGateInputs() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/admin/announcement-source-rule-releases/{releaseId}/publication",
                        RELEASE_ID
                )
                        .with(user(principal("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changeReason": "게시 승인"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void previewEndpointIsAdminOnly() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/admin/announcement-source-rule-releases/{releaseId}/preview",
                        RELEASE_ID
                )
                        .with(user(principal("OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 1,
                                  "title": "소상공인 정책자금",
                                  "bodyText": "소상공인 정책자금"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void insertGoldenSetRunReturnsServerGeneratedRunId() throws Exception {
        when(ruleReleaseService.insertGoldenSetRun(any(), eq(RELEASE_ID), any()))
                .thenReturn(new AnnouncementSourceRuleGoldenSetRunResponse(
                        "GOLDEN-SERVER-RUN",
                        "a".repeat(64),
                        20
                ));

        mockMvc.perform(post(
                        "/api/v1/admin/announcement-source-rule-releases/{releaseId}/golden-set-runs",
                        RELEASE_ID
                )
                        .with(user(principal("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goldenSetRunId").value("GOLDEN-SERVER-RUN"))
                .andExpect(jsonPath("$.data.goldenCaseCount").value(20));
    }

    private AuthenticatedUserDetails principal(String role) {
        UUID userId = "ADMIN".equals(role)
                ? UUID.fromString("72000000-0000-0000-0000-000000000002")
                : UUID.fromString("72000000-0000-0000-0000-000000000003");
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        userId,
                        role.toLowerCase() + "01",
                        "unused",
                        role,
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of(role)
        );
    }
}

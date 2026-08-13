package com.saneb.domain.announcementsource.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceClassificationDetailsResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceClassificationManagementService;
import com.saneb.domain.announcementsource.service.AnnouncementSourceReclassificationService;
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
class AnnouncementSourceClassificationControllerSmokeTest {

    private static final UUID SOURCE_ID = UUID.fromString("97000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AnnouncementSourceClassificationManagementService managementService;
    @MockitoBean private AnnouncementSourceReclassificationService reclassificationService;

    @Test
    void classificationDetailsAllowsInternalReadRoleAndKeepsWrapper() throws Exception {
        AnnouncementSourceClassificationDetailsResponse response =
                org.mockito.Mockito.mock(AnnouncementSourceClassificationDetailsResponse.class);
        when(managementService.selectClassificationDetails(SOURCE_ID)).thenReturn(response);

        mockMvc.perform(get(
                        "/api/v1/admin/announcement-sources/{sourceId}/classification", SOURCE_ID
                ).with(user(principal("APPROVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void reclassificationRejectsOperator() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/admin/announcement-sources/{sourceId}/reclassifications", SOURCE_ID
                )
                        .with(user(principal("OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reclassificationRequiresConcurrencyAndReasonFields() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/admin/announcement-sources/{sourceId}/reclassifications", SOURCE_ID
                )
                        .with(user(principal("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleReleaseId\":\"97000000-0000-0000-0000-000000000002\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void reclassificationReturnsApiWrapperForAdmin() throws Exception {
        AnnouncementSourceClassificationDetailsResponse response =
                org.mockito.Mockito.mock(AnnouncementSourceClassificationDetailsResponse.class);
        when(reclassificationService.insertReclassification(any(), eq(SOURCE_ID), any()))
                .thenReturn(response);

        mockMvc.perform(post(
                        "/api/v1/admin/announcement-sources/{sourceId}/reclassifications", SOURCE_ID
                )
                        .with(user(principal("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String validRequest() {
        return """
                {
                  "ruleReleaseId": "97000000-0000-0000-0000-000000000002",
                  "expectedClassificationDecisionId": "97000000-0000-0000-0000-000000000003",
                  "expectedVersion": 1,
                  "changeReason": "규칙 보정 후 명시적 재분류"
                }
                """;
    }

    private AuthenticatedUserDetails principal(String role) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.randomUUID(), "tester", "password-hash", "테스터", "ACTIVE",
                        false, null, null, null
                ),
                List.of(role)
        );
    }
}

package com.saneb.domain.announcementsource.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.announcementsource.dto.AnnouncementSourceReclassificationRunResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceReclassificationRunService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
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
class AnnouncementSourceReclassificationRunControllerSmokeTest {

    private static final UUID RUN_ID = UUID.fromString("74000000-0000-0000-0000-000000000001");
    private static final UUID RELEASE_ID = UUID.fromString("74000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementSourceReclassificationRunService service;

    @Test
    void previewRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcement-source-reclassification-runs/previews")
                        .with(user(principal("OPERATOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(previewJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateBoundedPreview() throws Exception {
        when(service.insertPreviewRun(any(), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/admin/announcement-source-reclassification-runs/previews")
                        .with(user(principal("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(previewJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runStatusCode").value("PREVIEW_PENDING"))
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    @Test
    void previewRejectsInvalidPeriodBeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcement-source-reclassification-runs/previews")
                        .with(user(principal("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleReleaseId": "%s",
                                  "collectedFrom": "2026-08-18",
                                  "collectedTo": "2026-08-01",
                                  "maximumCount": 500,
                                  "batchSize": 50,
                                  "changeReason": "운영 영향도 확인"
                                }
                                """.formatted(RELEASE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void operatorCanReadRunStatus() throws Exception {
        when(service.selectRunDetails(RUN_ID)).thenReturn(response());

        mockMvc.perform(get("/api/v1/admin/announcement-source-reclassification-runs/{runId}", RUN_ID)
                        .with(user(principal("OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value(RUN_ID.toString()));
    }

    private String previewJson() {
        return """
                {
                  "ruleReleaseId": "%s",
                  "providerCode": "BIZINFO",
                  "includeLinkedAnnouncements": false,
                  "maximumCount": 500,
                  "batchSize": 50,
                  "changeReason": "운영 영향도 확인"
                }
                """.formatted(RELEASE_ID);
    }

    private AnnouncementSourceReclassificationRunResponse response() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-18T12:00:00+09:00");
        return new AnnouncementSourceReclassificationRunResponse(
                RUN_ID, RELEASE_ID, "CLASSIFICATION-V1", "PREVIEW_PENDING", "BIZINFO",
                null, null, false, 500, 50, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0,
                1, now, now, null, null, null
        );
    }

    private AuthenticatedUserDetails principal(String role) {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.randomUUID(), role.toLowerCase() + "01", "unused", role,
                        "ACTIVE", false, null, null, null
                ),
                List.of(role)
        );
    }
}

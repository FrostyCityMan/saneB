package com.saneb.domain.announcementsource.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.saneb.domain.announcementsource.dto.SourceBodyRefreshResponses;
import com.saneb.domain.announcementsource.service.AnnouncementSourceBodyRefreshService;
import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementSourceBodyRefreshControllerSmokeTest {
    private static final UUID SOURCE=UUID.randomUUID();
    private static final UUID PREVIEW=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-sources/"+SOURCE+"/body-refresh-previews";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementSourceBodyRefreshService service;

    @Test void bothMutationsRequireCsrf() throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post(ROOT+"/"+PREVIEW+"/apply").with(user("qa").roles("ADMIN"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER","APPROVER"})
    void readersCannotPreviewOrApply(String role) throws Exception {
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(ROOT+"/"+PREVIEW+"/apply").with(user("qa").roles(role)).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR"})
    void permittedRolesReceiveNoStoreWrapper(String role) throws Exception {
        when(service.insertPreview(any(),eq(SOURCE))).thenReturn(new SourceBodyRefreshResponses.Preview(
                PREVIEW,SOURCE,"기존 본문","정제 본문","REVIEW_REQUIRED","REVIEW_REQUIRED","BODY_GROUP_A_MATCHED",
                List.of(),List.of(),OffsetDateTime.now().plusMinutes(30),"a".repeat(64),true));
        mvc.perform(post(ROOT).with(user("qa").roles(role)).with(csrf()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.isChanged").value(true))
                .andExpect(jsonPath("$.data.previewId").value(PREVIEW.toString()));
        verify(service).insertPreview(any(),eq(SOURCE));
        UUID evaluation=UUID.randomUUID();
        when(service.savePreview(any(),eq(SOURCE),eq(PREVIEW))).thenReturn(
                new SourceBodyRefreshResponses.Applied(PREVIEW,SOURCE,evaluation,"APPLIED"));
        mvc.perform(post(ROOT+"/"+PREVIEW+"/apply").with(user("qa").roles(role)).with(csrf()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.evaluationId").value(evaluation.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("APPLIED"));
        verify(service).savePreview(any(),eq(SOURCE),eq(PREVIEW));
    }

    @Test void anonymousCannotPreview() throws Exception {
        mvc.perform(post(ROOT).with(csrf())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void malformedPreviewReturnsActionableValidation() throws Exception {
        mvc.perform(post(ROOT+"/invalid/apply").with(user("qa").roles("ADMIN")).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("UUID")));
        verifyNoInteractions(service);
    }
}

package com.saneb.domain.announcementsource.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.domain.announcementsource.dto.AnnouncementSourceExclusionSummaryResponse;
import com.saneb.domain.announcementsource.service.AnnouncementSourceExclusionService;
import java.time.OffsetDateTime;
import java.util.List;
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
class AnnouncementSourceExclusionControllerSmokeTest {
    private static final String PATH="/api/v2/admin/announcement-source-exclusions/summary";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementSourceExclusionService service;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void allowedRolesReceiveOnlySummary(String role) throws Exception {
        when(service.selectSummaryDetails()).thenReturn(new AnnouncementSourceExclusionSummaryResponse(2,1,List.of(),List.of(),
                "PROVIDER_IDENTITY_UNIQUE","RETAINED_PRODUCTION_BY_LATEST_RUN",OffsetDateTime.parse("2026-10-12T00:00:00Z")));
        mvc.perform(get(PATH).with(user("qa").roles(role))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uniqueExcludedCount").value(2))
                .andExpect(jsonPath("$.data.unknownPurposeCount").value(1))
                .andExpect(jsonPath("$.data.identityHash").doesNotExist()).andExpect(jsonPath("$.data.title").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void otherRolesCannotQuery(String role) throws Exception {
        mvc.perform(get(PATH).with(user("qa").roles(role))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void unauthenticatedCannotQuery() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized()); verifyNoInteractions(service);
    }
}

package com.saneb.domain.applicationprogress.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressDetailsResponse;
import com.saneb.domain.applicationprogress.service.ApplicationProgressService;
import com.saneb.domain.auth.dto.AuthMeResponse;
import com.saneb.domain.auth.service.AuthService;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.service.MatchingService;
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
class ApplicationProgressViewControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PROGRESS_ID = UUID.fromString("61000000-0000-0000-0000-000000000001");
    private static final UUID MATCHING_CASE_ID = UUID.fromString("61000000-0000-0000-0000-000000000002");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("61000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ApplicationProgressService applicationProgressService;

    @MockitoBean
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        when(authService.selectAuthMe(any())).thenReturn(authMe());
        when(applicationProgressService.selectApplicationProgressList(
                any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)
        )).thenReturn(PageResponse.of(List.of(), 1, 20, 0));
        when(matchingService.selectMatchingCaseList(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(PageResponse.of(List.of(matchingSummary()), 1, 20, 1));
        when(applicationProgressService.insertApplicationProgress(any(), any()))
                .thenReturn(progressDetails());
    }

    @Test
    void selectApplicationProgressListPageShowsStartableMatchingCases() throws Exception {
        mockMvc.perform(get("/app/application-progresses")
                        .with(user("local_user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("app/application-progress-detail"))
                .andExpect(content().string(containsString("신청 가능한 공고")))
                .andExpect(content().string(containsString("검증 없이 진행 가능")))
                .andExpect(content().string(containsString("신청 시작")));
    }

    @Test
    void insertApplicationProgressFromViewRedirectsToCreatedProgress() throws Exception {
        mockMvc.perform(post("/app/application-progresses/start")
                        .with(user("local_user").roles("USER"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("matchingCaseId", MATCHING_CASE_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/application-progresses/" + PROGRESS_ID));
    }

    private AuthMeResponse authMe() {
        return new AuthMeResponse(
                USER_ID,
                "local_user",
                "Local User",
                List.of("USER"),
                "USER",
                "/app/dashboard",
                false,
                new AuthMeResponse.ProfileResponse(null, null, null)
        );
    }

    private MatchingCaseSummaryResponse matchingSummary() {
        OffsetDateTime now = OffsetDateTime.now();
        return new MatchingCaseSummaryResponse(
                MATCHING_CASE_ID,
                ANNOUNCEMENT_ID,
                USER_ID,
                null,
                "MATCHED",
                null,
                now,
                now,
                now
        );
    }

    private ApplicationProgressDetailsResponse progressDetails() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ApplicationProgressDetailsResponse(
                PROGRESS_ID,
                MATCHING_CASE_ID,
                ANNOUNCEMENT_ID,
                USER_ID,
                null,
                "READY",
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now,
                List.of(),
                List.of()
        );
    }
}

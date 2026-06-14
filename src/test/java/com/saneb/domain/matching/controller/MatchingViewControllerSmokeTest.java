package com.saneb.domain.matching.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.service.MatchingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class MatchingViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchingService matchingService;

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectMatchingCasePageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/matching/cases"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/matching-cases"))
                .andExpect(content().string(containsString("매칭 관리")))
                .andExpect(content().string(containsString("data-matching-app")))
                .andExpect(content().string(containsString("/api/v1/matching/cases")))
                .andExpect(content().string(containsString("data-lookup-open=\"announcement\"")))
                .andExpect(content().string(containsString("data-lookup-open=\"member\"")))
                .andExpect(content().string(containsString("/api/v1/matching/cases/member-lookups")))
                .andExpect(content().string(not(containsString("검증 ID"))))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "approver01", roles = "APPROVER")
    void selectMatchingCasePageAllowsApprover() throws Exception {
        mockMvc.perform(get("/app/matching/cases"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/matching-cases"));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectMatchingCasePageRejectsUser() throws Exception {
        mockMvc.perform(get("/app/matching/cases"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectBasicMatchingCandidatePageReturnsUserCandidateView() throws Exception {
        when(matchingService.selectMyBasicMatchingCaseList(any(), eq(1), eq(50)))
                .thenReturn(PageResponse.of(List.of(basicCandidate()), 1, 50, 1));

        mockMvc.perform(get("/app/matching/basic-candidates"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/basic-matching-candidates"))
                .andExpect(content().string(containsString("현재 매칭 공고")))
                .andExpect(content().string(containsString("테스트 현재 공고")))
                .andExpect(content().string(containsString("ANN-000001")))
                .andExpect(content().string(containsString("구독 결제하기")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "operator01", roles = "OPERATOR")
    void selectBasicMatchingCandidatePageRejectsOperator() throws Exception {
        mockMvc.perform(get("/app/matching/basic-candidates"))
                .andExpect(status().isForbidden());
    }

    private MatchingCaseSummaryResponse basicCandidate() {
        return new MatchingCaseSummaryResponse(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                "MCH-000001",
                UUID.fromString("50000000-0000-0000-0000-000000000002"),
                "ANN-000001",
                UUID.fromString("50000000-0000-0000-0000-000000000003"),
                "USR-000001",
                null,
                null,
                "MATCHED",
                null,
                "BASIC",
                "BASIC_INFO",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "테스트 현재 공고",
                "테스트 기관",
                "BUSINESS",
                new BigDecimal("1000000"),
                new BigDecimal("3000000"),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "user01",
                "사용자",
                false
        );
    }
}

package com.saneb.domain.matching.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.matching.dto.MatchingCaseDetailsResponse;
import com.saneb.domain.matching.dto.MatchingCaseSummaryResponse;
import com.saneb.domain.matching.dto.MatchingMemberLookupResponse;
import com.saneb.domain.matching.dto.MatchingResultDetailResponse;
import com.saneb.domain.matching.service.MatchingService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class MatchingControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID MATCHING_CASE_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID MEMBER_USER_ID = UUID.fromString("50000000-0000-0000-0000-000000000003");
    private static final UUID VERIFICATION_ID = UUID.fromString("50000000-0000-0000-0000-000000000004");
    private static final UUID RESULT_DETAIL_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchingService matchingService;

    @Test
    void insertMatchingCaseReturnsApiResponse() throws Exception {
        when(matchingService.insertMatchingCase(any(), any())).thenReturn(details("MATCHED", null));

        mockMvc.perform(post("/api/v1/matching/cases")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchingCaseId").value(MATCHING_CASE_ID.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("MATCHED"));
    }

    @Test
    void selectMatchingCaseListReturnsPagedApiResponse() throws Exception {
        when(matchingService.selectMatchingCaseList(any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(summary("REVIEW_REQUIRED", "NEEDS_REVIEW")), 1, 20, 1));

        mockMvc.perform(get("/api/v1/matching/cases")
                        .with(user(operatorPrincipal()))
                        .param("statusCode", "REVIEW_REQUIRED")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].statusCode").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void selectMatchingMemberLookupListReturnsPagedApiResponse() throws Exception {
        when(matchingService.selectMatchingMemberLookupList(any(), eq(1), eq(10)))
                .thenReturn(PageResponse.of(List.of(new MatchingMemberLookupResponse(
                        MEMBER_USER_ID,
                        "user01",
                        "사용자",
                        "ACTIVE",
                        OffsetDateTime.now()
                )), 1, 10, 1));

        mockMvc.perform(get("/api/v1/matching/cases/member-lookups")
                        .with(user(operatorPrincipal()))
                        .param("keyword", "user")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].userId").value(MEMBER_USER_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].loginId").value("user01"));
    }

    @Test
    void selectMatchingCaseDetailsReturnsApiResponse() throws Exception {
        when(matchingService.selectMatchingCaseDetails(MATCHING_CASE_ID)).thenReturn(details("BLOCKED", "POLICY_FUND_RESTRICTED"));

        mockMvc.perform(get("/api/v1/matching/cases/{matchingCaseId}", MATCHING_CASE_ID)
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.blockedReasonCode").value("POLICY_FUND_RESTRICTED"));
    }

    @Test
    void selectMatchingResultDetailListReturnsApiResponse() throws Exception {
        when(matchingService.selectMatchingResultDetailList(MATCHING_CASE_ID)).thenReturn(List.of(
                new MatchingResultDetailResponse(
                        RESULT_DETAIL_ID,
                        MATCHING_CASE_ID,
                        "APPLICATION",
                        "RESTRICTION_FLAGS",
                        "PASS",
                        "NONE",
                        "NO_BLOCKING_RESTRICTION",
                        "No checked restriction flag.",
                        OffsetDateTime.now()
                )
        ));

        mockMvc.perform(get("/api/v1/matching/cases/{matchingCaseId}/results", MATCHING_CASE_ID)
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].resultCode").value("PASS"))
                .andExpect(jsonPath("$.data[0].conditionKey").value("RESTRICTION_FLAGS"));
    }

    @Test
    void updateMatchingCaseStatusReturnsApiResponse() throws Exception {
        when(matchingService.updateMatchingCaseStatus(any(), eq(MATCHING_CASE_ID), any()))
                .thenReturn(details("NOT_MATCHED", "MANUAL_REVIEW"));

        mockMvc.perform(patch("/api/v1/matching/cases/{matchingCaseId}/status", MATCHING_CASE_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "NOT_MATCHED",
                                  "blockedReasonCode": "MANUAL_REVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("NOT_MATCHED"));
    }

    private MatchingCaseSummaryResponse summary(String statusCode, String blockedReasonCode) {
        OffsetDateTime now = OffsetDateTime.now();
        return new MatchingCaseSummaryResponse(
                MATCHING_CASE_ID,
                ANNOUNCEMENT_ID,
                MEMBER_USER_ID,
                VERIFICATION_ID,
                statusCode,
                blockedReasonCode,
                now,
                now,
                now
        );
    }

    private MatchingCaseDetailsResponse details(String statusCode, String blockedReasonCode) {
        OffsetDateTime now = OffsetDateTime.now();
        return new MatchingCaseDetailsResponse(
                MATCHING_CASE_ID,
                ANNOUNCEMENT_ID,
                MEMBER_USER_ID,
                VERIFICATION_ID,
                statusCode,
                blockedReasonCode,
                now,
                USER_ID,
                now,
                now,
                now
        );
    }

    private AuthenticatedUserDetails operatorPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "local_operator",
                        "password-hash",
                        "Local Operator",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("OPERATOR")
        );
    }

    private String createRequest() {
        return """
                {
                  "announcementId": "%s",
                  "memberUserId": "%s"
                }
                """.formatted(ANNOUNCEMENT_ID, MEMBER_USER_ID);
    }
}

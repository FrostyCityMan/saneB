package com.saneb.domain.applicationprogress.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressDetailsResponse;
import com.saneb.domain.applicationprogress.dto.ApplicationProgressSummaryResponse;
import com.saneb.domain.applicationprogress.service.ApplicationProgressService;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class ApplicationProgressControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PROGRESS_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID MATCHING_CASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("60000000-0000-0000-0000-000000000003");
    private static final UUID MEMBER_USER_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID STEP_ID = UUID.fromString("60000000-0000-0000-0000-000000000005");
    private static final UUID STEP_STATE_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final UUID STEP_DOCUMENT_ID = UUID.fromString("60000000-0000-0000-0000-000000000007");
    private static final UUID CHECKLIST_ID = UUID.fromString("60000000-0000-0000-0000-000000000008");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationProgressService applicationProgressService;

    @Test
    void insertApplicationProgressReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(applicationProgressService.insertApplicationProgress(any(), any()))
                .thenReturn(details("READY"));

        mockMvc.perform(post("/api/v1/application-progresses")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchingCaseId": "%s"
                                }
                                """.formatted(MATCHING_CASE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.progressId").value(PROGRESS_ID.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("READY"));
    }

    @Test
    void selectApplicationProgressListReturnsPagedApiResponse() throws Exception {
        org.mockito.Mockito.when(applicationProgressService.selectApplicationProgressList(
                        any(), any(), any(), any(), any(), eq(1), eq(20)
                ))
                .thenReturn(PageResponse.of(List.of(summary("IN_PROGRESS")), 1, 20, 1));

        mockMvc.perform(get("/api/v1/application-progresses")
                        .with(user(operatorPrincipal()))
                        .param("statusCode", "IN_PROGRESS")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].progressId").value(PROGRESS_ID.toString()))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void selectApplicationProgressDetailsReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(applicationProgressService.selectApplicationProgressDetails(PROGRESS_ID))
                .thenReturn(details("READY"));

        mockMvc.perform(get("/api/v1/application-progresses/{progressId}", PROGRESS_ID)
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stepStates[0].stepId").value(STEP_ID.toString()))
                .andExpect(jsonPath("$.data.stepButtons[0].buttonLabel").value("진행 원함"));
    }

    @Test
    void updateStepActionReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(applicationProgressService.updateProgressStepAction(any(), eq(PROGRESS_ID), eq(STEP_ID), any()))
                .thenReturn(details("IN_PROGRESS"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/steps/{stepId}/action", PROGRESS_ID, STEP_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buttonCode": "WANTS_TO_PROGRESS",
                                  "input": {
                                    "memo": "ok"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("IN_PROGRESS"));
    }

    @Test
    void saveDocumentsReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(applicationProgressService.saveProgressStepDocuments(any(), eq(PROGRESS_ID), eq(STEP_ID), any()))
                .thenReturn(details("READY"));

        mockMvc.perform(put("/api/v1/application-progresses/{progressId}/steps/{stepId}/documents", PROGRESS_ID, STEP_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documents": [
                                    {
                                      "stepDocumentId": "%s",
                                      "checked": true
                                    }
                                  ]
                                }
                                """.formatted(STEP_DOCUMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checklists[0].checked").value(true));
    }

    @Test
    void updateReceiptAndResultReturnApiResponse() throws Exception {
        org.mockito.Mockito.when(applicationProgressService.updateProgressReceipt(any(), eq(PROGRESS_ID), any()))
                .thenReturn(details("WAITING_RESULT"));
        org.mockito.Mockito.when(applicationProgressService.updateProgressResult(any(), eq(PROGRESS_ID), any()))
                .thenReturn(details("APPROVED"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/receipt", PROGRESS_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiptNo": "A-2026-0001",
                                  "receiptDate": "2026-06-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("WAITING_RESULT"));

        mockMvc.perform(patch("/api/v1/application-progresses/{progressId}/result", PROGRESS_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resultCode": "APPROVED",
                                  "resultNote": "Approved",
                                  "resultDate": "2026-07-01",
                                  "receivedAmount": 7000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("APPROVED"));
    }

    private ApplicationProgressSummaryResponse summary(String statusCode) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ApplicationProgressSummaryResponse(
                PROGRESS_ID,
                MATCHING_CASE_ID,
                ANNOUNCEMENT_ID,
                MEMBER_USER_ID,
                "APP-000001",
                "MCH-000001",
                "ANN-000001",
                "USR-000001",
                STEP_ID,
                statusCode,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                now,
                now
        );
    }

    private ApplicationProgressDetailsResponse details(String statusCode) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ApplicationProgressDetailsResponse(
                PROGRESS_ID,
                MATCHING_CASE_ID,
                ANNOUNCEMENT_ID,
                MEMBER_USER_ID,
                "APP-000001",
                "MCH-000001",
                "ANN-000001",
                "USR-000001",
                STEP_ID,
                statusCode,
                "A-2026-0001",
                LocalDate.of(2026, 6, 15),
                "APPROVED".equals(statusCode) ? "APPROVED" : null,
                "Approved",
                "APPROVED".equals(statusCode) ? LocalDate.of(2026, 7, 1) : null,
                "APPROVED".equals(statusCode) ? new BigDecimal("7000000") : null,
                now,
                now,
                List.of(new ApplicationProgressDetailsResponse.StepStateResponse(
                        STEP_STATE_ID,
                        STEP_ID,
                        1,
                        "Guide Sent",
                        "READY",
                        now,
                        null
                )),
                List.of(new ApplicationProgressDetailsResponse.ChecklistResponse(
                        CHECKLIST_ID,
                        STEP_DOCUMENT_ID,
                        STEP_ID,
                        "BUSINESS_REGISTRATION",
                        true,
                        true,
                        now,
                        USER_ID
                )),
                List.of(new ApplicationProgressDetailsResponse.StepButtonResponse(
                        STEP_ID,
                        "WANTS_TO_PROGRESS",
                        "진행 원함",
                        "MOVE_NEXT",
                        null,
                        1
                ))
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

    private AuthenticatedUserDetails userPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        MEMBER_USER_ID,
                        "local_user",
                        "password-hash",
                        "Local User",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("USER")
        );
    }
}

package com.saneb.domain.announcement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.announcement.dao.AnnouncementDao;
import com.saneb.domain.announcement.vo.AnnouncementApprovalDecisionCommand;
import com.saneb.domain.announcement.vo.AnnouncementApprovalRequestCommand;
import com.saneb.domain.announcement.vo.AnnouncementApprovalStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementManualStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementStandardDocumentFieldRow;
import com.saneb.domain.announcement.vo.AnnouncementSummaryRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID STANDARD_FIELD_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnnouncementDao announcementDao;

    @TestConfiguration
    static class AnnouncementControllerSmokeTestConfig {

        @Bean
        @Primary
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @Test
    void selectAnnouncementListReturnsPagedApiResponse() throws Exception {
        when(announcementDao.selectAnnouncementCount(any())).thenReturn(1L);
        when(announcementDao.selectAnnouncementList(any())).thenReturn(List.of(summaryRow()));

        mockMvc.perform(get("/api/v1/announcements")
                        .param("page", "1")
                        .param("size", "20")
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].announcementId").value(ANNOUNCEMENT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].title").value("Operating Capital Support"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void insertAnnouncementReturnsApiResponse() throws Exception {
        stubDetails();

        mockMvc.perform(post("/api/v1/announcements")
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest("Operating Capital Support")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementId").value(ANNOUNCEMENT_ID.toString()))
                .andExpect(jsonPath("$.data.approvalStatusCode").value("DRAFT"))
                .andExpect(jsonPath("$.data.options[0].optionCode").value("ONLINE"));

        verify(announcementDao).insertAnnouncement(any());
        verify(announcementDao).insertAnnouncementOption(any());
    }

    @Test
    void updateAnnouncementReturnsApiResponse() throws Exception {
        stubDetails();
        when(announcementDao.updateAnnouncement(any())).thenReturn(1);

        mockMvc.perform(put("/api/v1/announcements/{announcementId}", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest("Updated Capital Support")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementId").value(ANNOUNCEMENT_ID.toString()));

        verify(announcementDao).updateAnnouncement(any());
        verify(announcementDao).deleteAnnouncementOptions(ANNOUNCEMENT_ID);
    }

    @Test
    void updateAnnouncementConditionsReturnsApiResponse() throws Exception {
        stubDetails();

        mockMvc.perform(put("/api/v1/announcements/{announcementId}/conditions", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "industryConditions": [
                                    {
                                      "conditionTypeCode": "INCLUDE",
                                      "ksicCode": "47911"
                                    },
                                    {
                                      "conditionTypeCode": "EXCLUDE",
                                      "ksicCode": "56121"
                                    }
                                  ],
                                  "numericConditions": [
                                    {
                                      "conditionScopeCode": "BUSINESS",
                                      "conditionKey": "ANNUAL_REVENUE",
                                      "comparatorCode": "LTE",
                                      "valueNumber": 300000000,
                                      "unitCode": "KRW"
                                    },
                                    {
                                      "conditionScopeCode": "BUSINESS",
                                      "conditionKey": "EMPLOYEE_COUNT",
                                      "comparatorCode": "LTE",
                                      "valueNumber": 5,
                                      "unitCode": "COUNT"
                                    },
                                    {
                                      "conditionScopeCode": "PERSONAL",
                                      "conditionKey": "AGE",
                                      "comparatorCode": "GTE",
                                      "valueNumber": 19,
                                      "unitCode": "YEAR"
                                    }
                                  ],
                                  "optionConditions": [
                                    {
                                      "conditionScopeCode": "BUSINESS",
                                      "conditionKey": "BUSINESS_TYPE",
                                      "optionCode": "SOLE_PROPRIETOR"
                                    },
                                    {
                                      "conditionScopeCode": "BUSINESS",
                                      "conditionKey": "BUSINESS_STAGE",
                                      "optionCode": "OPERATING"
                                    },
                                    {
                                      "conditionScopeCode": "APPLICATION",
                                      "conditionKey": "APPLICATION_METHOD",
                                      "optionCode": "ONLINE"
                                    }
                                  ],
                                  "documentRequirements": [
                                    {
                                      "documentTypeCode": "BUSINESS_REGISTRATION",
                                      "required": true,
                                      "sortOrder": 1
                                    },
                                    {
                                      "documentTypeCode": "INCOME_CERTIFICATE",
                                      "required": true,
                                      "sortOrder": 2
                                    },
                                    {
                                      "documentTypeCode": "HEALTH_INSURANCE",
                                      "required": false,
                                      "sortOrder": 3
                                    },
                                    {
                                      "documentTypeCode": "CUSTOM_DOCUMENT",
                                      "required": true,
                                      "sortOrder": 4
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(announcementDao, times(2)).insertAnnouncementIndustryCondition(any());
        verify(announcementDao, times(3)).insertAnnouncementNumericCondition(any());
        verify(announcementDao, times(3)).insertAnnouncementOptionCondition(any());
        verify(announcementDao, times(4)).insertAnnouncementDocumentRequirement(any());
    }

    @Test
    void updateAnnouncementConditionsRejectsNonEligibleStandardField() throws Exception {
        stubDetails();
        when(announcementDao.selectStandardDocumentFieldDetails(STANDARD_FIELD_ID))
                .thenReturn(new AnnouncementStandardDocumentFieldRow(
                        STANDARD_FIELD_ID,
                        "BUSINESS_REGISTRATION",
                        "WORKPLACE_ADDRESS",
                        "사업장 주소",
                        "TEXT",
                        "BUSINESS",
                        false,
                        "INPUT_ONLY",
                        true
                ));

        mockMvc.perform(put("/api/v1/announcements/{announcementId}/conditions", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numericConditions": [
                                    {
                                      "standardFieldId": "%s",
                                      "conditionScopeCode": "BUSINESS",
                                      "conditionKey": "WORKPLACE_ADDRESS",
                                      "comparatorCode": "LTE",
                                      "valueNumber": 1
                                    }
                                  ]
                                }
                                """.formatted(STANDARD_FIELD_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateAnnouncementStepsReturnsApiResponse() throws Exception {
        stubDetails();

        mockMvc.perform(put("/api/v1/announcements/{announcementId}/steps", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steps": [
                                    { "stepOrder": 1, "stepName": "안내 발송", "guideMessage": "현재 사업 정보 기준으로 진행 가능한 항목이 확인되었습니다.", "actionGuide": "진행 의사를 선택하세요.", "completionConditionCode": "BUTTON_CLICK", "nextConditionCode": "진행 의사 확인", "active": true, "buttons": [
                                      { "buttonCode": "WANT_TO_PROCEED", "buttonLabel": "진행 원함", "buttonActionCode": "MOVE_NEXT", "sortOrder": 1 },
                                      { "buttonCode": "ALREADY_RECEIVED", "buttonLabel": "이미 지원받음", "buttonActionCode": "STOP_PROGRESS", "sortOrder": 2 },
                                      { "buttonCode": "ALREADY_IN_PROGRESS", "buttonLabel": "이미 진행중", "buttonActionCode": "STOP_PROGRESS", "sortOrder": 3 },
                                      { "buttonCode": "NOT_INTERESTED", "buttonLabel": "관심없음", "buttonActionCode": "STOP_PROGRESS", "sortOrder": 4 }
                                    ], "documents": [] },
                                    { "stepOrder": 2, "stepName": "서류 안내", "guideMessage": "진행에 필요한 서류를 준비합니다.", "actionGuide": "필수 서류가 모두 준비되면 서류 준비 완료를 선택하세요.", "completionConditionCode": "ALL_REQUIRED_DOCUMENTS_CHECKED", "nextConditionCode": "필수 서류 전체 확인", "active": true, "buttons": [
                                      { "buttonCode": "DOCUMENTS_READY", "buttonLabel": "서류 준비 완료", "buttonActionCode": "MOVE_NEXT", "sortOrder": 1 }
                                    ], "documents": [
                                      { "documentTypeCode": "BUSINESS_REGISTRATION", "required": true, "sortOrder": 1 },
                                      { "documentTypeCode": "VAT_TAX_BASE", "required": true, "sortOrder": 2 },
                                      { "documentTypeCode": "RESIDENT_REGISTRATION", "required": true, "sortOrder": 3 },
                                      { "documentTypeCode": "FAMILY_RELATION", "required": true, "sortOrder": 4 }
                                    ] },
                                    { "stepOrder": 3, "stepName": "접수 단계", "guideMessage": "접수 전 최종 확인을 진행합니다.", "actionGuide": "접수 진행 여부를 선택하세요.", "completionConditionCode": "BUTTON_CLICK", "nextConditionCode": "접수 진행 의사 확인", "active": true, "buttons": [
                                      { "buttonCode": "START_RECEIPT", "buttonLabel": "접수 진행하기", "buttonActionCode": "MOVE_NEXT", "sortOrder": 1 },
                                      { "buttonCode": "ALREADY_RECEIVED", "buttonLabel": "이미 지원받음", "buttonActionCode": "STOP_PROGRESS", "sortOrder": 2 },
                                      { "buttonCode": "STOP_APPLICATION", "buttonLabel": "진행 중단", "buttonActionCode": "STOP_PROGRESS", "sortOrder": 3 }
                                    ], "documents": [] },
                                    { "stepOrder": 4, "stepName": "접수 진행", "guideMessage": "접수 정보를 저장합니다.", "actionGuide": "접수 정보를 저장한 뒤 접수 완료를 선택하세요.", "completionConditionCode": "RECEIPT_SAVED", "nextConditionCode": "접수 정보 저장", "active": true, "buttons": [
                                      { "buttonCode": "RECEIPT_DONE", "buttonLabel": "접수 완료", "buttonActionCode": "MOVE_NEXT", "sortOrder": 1 }
                                    ], "documents": [] },
                                    { "stepOrder": 5, "stepName": "접수 완료", "guideMessage": "결과 대기 상태입니다.", "actionGuide": "결과를 확인할 수 있으면 결과 입력하기를 선택하세요.", "completionConditionCode": "BUTTON_CLICK", "nextConditionCode": "결과 입력 가능", "active": true, "buttons": [
                                      { "buttonCode": "OPEN_RESULT_INPUT", "buttonLabel": "결과 입력하기", "buttonActionCode": "MOVE_NEXT", "sortOrder": 1 }
                                    ], "documents": [] },
                                    { "stepOrder": 6, "stepName": "결과 입력", "guideMessage": "최종 결과를 저장합니다.", "actionGuide": "결과 정보를 저장한 뒤 결과 저장을 선택하세요.", "completionConditionCode": "RESULT_SAVED", "nextConditionCode": "최종 결과 저장", "active": true, "buttons": [
                                      { "buttonCode": "SAVE_RESULT", "buttonLabel": "결과 저장", "buttonActionCode": "MOVE_NEXT", "sortOrder": 1 }
                                    ], "documents": [] }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(announcementDao, times(6)).insertAnnouncementProgressStep(any());
        verify(announcementDao, times(11)).insertAnnouncementStepButton(any());
        verify(announcementDao, times(4)).insertAnnouncementStepDocument(any());
    }

    @Test
    void updateAnnouncementManualStatusReturnsApiResponse() throws Exception {
        stubDetails();
        when(announcementDao.updateAnnouncementManualStatus(any())).thenReturn(1);

        mockMvc.perform(patch("/api/v1/announcements/{announcementId}/manual-status", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manualStatusCode": "PAUSED",
                                  "reason": "Operation test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(announcementDao).updateAnnouncementManualStatus(any(AnnouncementManualStatusCommand.class));
        verify(announcementDao).insertAnnouncementStatusHistory(any());
    }

    @Test
    void insertAnnouncementApprovalRequestReturnsApiResponse() throws Exception {
        stubDetails();
        when(announcementDao.updateAnnouncementApprovalStatus(any())).thenReturn(1);

        mockMvc.perform(post("/api/v1/announcements/{announcementId}/approval-requests", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestNote": "승인 요청"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(announcementDao).insertAnnouncementApprovalRequest(any(AnnouncementApprovalRequestCommand.class));
        verify(announcementDao).updateAnnouncementApprovalStatus(any(AnnouncementApprovalStatusCommand.class));
    }

    @Test
    void updateAnnouncementApprovalReturnsApiResponse() throws Exception {
        stubDetailsWithApprovalStatuses("REQUESTED", "APPROVED");
        when(announcementDao.selectRequestedApprovalRequestCount(ANNOUNCEMENT_ID)).thenReturn(1L);
        when(announcementDao.updateAnnouncementApprovalDecision(any())).thenReturn(1);
        when(announcementDao.updateAnnouncementApprovalStatus(any())).thenReturn(1);

        mockMvc.perform(patch("/api/v1/announcements/{announcementId}/approval", ANNOUNCEMENT_ID)
                        .with(user(approverPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvalStatusCode": "APPROVED",
                                  "decisionNote": "승인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvalStatusCode").value("APPROVED"));

        verify(announcementDao).updateAnnouncementApprovalDecision(any(AnnouncementApprovalDecisionCommand.class));
        verify(announcementDao).updateAnnouncementApprovalStatus(any(AnnouncementApprovalStatusCommand.class));
    }

    private void stubDetails() {
        stubDetailsWithApprovalStatuses("DRAFT");
    }

    private void stubDetailsWithApprovalStatuses(String firstStatus, String... followingStatuses) {
        AnnouncementDetailsRow[] rows = new AnnouncementDetailsRow[followingStatuses.length + 1];
        rows[0] = detailsRow(firstStatus);
        for (int index = 0; index < followingStatuses.length; index++) {
            rows[index + 1] = detailsRow(followingStatuses[index]);
        }
        if (rows.length == 1) {
            when(announcementDao.selectAnnouncementDetails(any())).thenReturn(rows[0]);
        } else {
            when(announcementDao.selectAnnouncementDetails(any())).thenReturn(
                    rows[0],
                    Arrays.copyOfRange(rows, 1, rows.length)
            );
        }
        when(announcementDao.selectAnnouncementOptionList(any())).thenReturn(List.of(
                new com.saneb.domain.announcement.vo.AnnouncementOptionRow("APPLICATION_METHOD", "ONLINE")
        ));
        when(announcementDao.selectAnnouncementIndustryConditionList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementNumericConditionList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementOptionConditionList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementDocumentRequirementList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementProgressStepList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementStepDocumentList(any())).thenReturn(List.of());
        when(announcementDao.selectAnnouncementStepButtonList(any())).thenReturn(List.of());
    }

    private AnnouncementSummaryRow summaryRow() {
        return new AnnouncementSummaryRow(
                ANNOUNCEMENT_ID,
                "ANN-000001",
                "BUSINESS",
                "Operating Capital Support",
                "Seoul City",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "NORMAL",
                "DRAFT",
                new BigDecimal("1000000"),
                new BigDecimal("5000000"),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private AnnouncementDetailsRow detailsRow(String approvalStatusCode) {
        return new AnnouncementDetailsRow(
                ANNOUNCEMENT_ID,
                "ANN-000001",
                "BUSINESS",
                "Operating Capital Support",
                "Seoul City",
                "MVP operation test announcement",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "NORMAL",
                approvalStatusCode,
                "VAT_TAX_BASE_ONLY",
                new BigDecimal("1000000"),
                new BigDecimal("5000000"),
                OffsetDateTime.now(),
                OffsetDateTime.now()
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

    private AuthenticatedUserDetails approverPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        USER_ID,
                        "local_approver",
                        "password-hash",
                        "Local Approver",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("APPROVER")
        );
    }

    private String saveRequest(String title) {
        return """
                {
                  "targetTypeCode": "BUSINESS",
                  "title": "%s",
                  "agencyName": "Seoul City",
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
}

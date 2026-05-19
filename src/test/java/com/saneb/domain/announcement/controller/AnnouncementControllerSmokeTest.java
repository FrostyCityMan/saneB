package com.saneb.domain.announcement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.saneb.domain.announcement.vo.AnnouncementDetailsRow;
import com.saneb.domain.announcement.vo.AnnouncementManualStatusCommand;
import com.saneb.domain.announcement.vo.AnnouncementSummaryRow;
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
                                    }
                                  ],
                                  "numericConditions": [
                                    {
                                      "conditionScopeCode": "BUSINESS",
                                      "conditionKey": "ANNUAL_REVENUE",
                                      "comparatorCode": "LTE",
                                      "valueNumber": 300000000,
                                      "unitCode": "KRW"
                                    }
                                  ],
                                  "optionConditions": [
                                    {
                                      "conditionScopeCode": "BUSINESS",
                                      "conditionKey": "BUSINESS_TYPE",
                                      "optionCode": "SOLE_PROPRIETOR"
                                    }
                                  ],
                                  "documentRequirements": [
                                    {
                                      "documentTypeCode": "BUSINESS_REGISTRATION",
                                      "required": true,
                                      "sortOrder": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(announcementDao).insertAnnouncementIndustryCondition(any());
        verify(announcementDao).insertAnnouncementNumericCondition(any());
        verify(announcementDao).insertAnnouncementOptionCondition(any());
        verify(announcementDao).insertAnnouncementDocumentRequirement(any());
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
                                    {
                                      "stepOrder": 1,
                                      "stepName": "Guide Sent",
                                      "guideMessage": "Check the guide.",
                                      "actionGuide": "Select next action.",
                                      "completionConditionCode": "BUTTON_CLICK",
                                      "nextConditionCode": "WANTS_TO_PROGRESS",
                                      "active": true,
                                      "buttons": [
                                        {
                                          "buttonCode": "WANTS_TO_PROGRESS",
                                          "buttonLabel": "Proceed",
                                          "buttonActionCode": "MOVE_NEXT",
                                          "sortOrder": 1
                                        }
                                      ],
                                      "documents": [
                                        {
                                          "documentTypeCode": "BUSINESS_REGISTRATION",
                                          "required": true,
                                          "sortOrder": 1
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(announcementDao).insertAnnouncementProgressStep(any());
        verify(announcementDao).insertAnnouncementStepButton(any());
        verify(announcementDao).insertAnnouncementStepDocument(any());
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

    private void stubDetails() {
        when(announcementDao.selectAnnouncementDetails(any())).thenReturn(detailsRow());
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

    private AnnouncementDetailsRow detailsRow() {
        return new AnnouncementDetailsRow(
                ANNOUNCEMENT_ID,
                "BUSINESS",
                "Operating Capital Support",
                "Seoul City",
                "MVP operation test announcement",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "NORMAL",
                "DRAFT",
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

package com.saneb.domain.dynamicinput.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.dynamicinput.dto.AnnouncementInputRequirementsResponse;
import com.saneb.domain.dynamicinput.dto.ApplicationInputValuesResponse;
import com.saneb.domain.dynamicinput.service.DynamicAnnouncementInputService;
import java.math.BigDecimal;
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
class DynamicAnnouncementInputControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final UUID REQUIREMENT_ID = UUID.fromString("80000000-0000-0000-0000-000000000002");
    private static final UUID OPTION_ID = UUID.fromString("80000000-0000-0000-0000-000000000003");
    private static final UUID PROGRESS_ID = UUID.fromString("80000000-0000-0000-0000-000000000004");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DynamicAnnouncementInputService dynamicAnnouncementInputService;

    @Test
    void selectAnnouncementInputRequirementsReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(dynamicAnnouncementInputService.selectAnnouncementInputRequirements(ANNOUNCEMENT_ID))
                .thenReturn(requirementsResponse());

        mockMvc.perform(get("/api/v1/announcements/{announcementId}/input-requirements", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.announcementId").value(ANNOUNCEMENT_ID.toString()))
                .andExpect(jsonPath("$.data.requirements[0].fieldKey").value("BUSINESS_PLACE"))
                .andExpect(jsonPath("$.data.requirements[0].options[0].optionCode").value("ONLINE"));
    }

    @Test
    void saveAnnouncementInputRequirementsReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(dynamicAnnouncementInputService.saveAnnouncementInputRequirements(
                        any(),
                        eq(ANNOUNCEMENT_ID),
                        any()
                ))
                .thenReturn(requirementsResponse());

        mockMvc.perform(put("/api/v1/announcements/{announcementId}/input-requirements", ANNOUNCEMENT_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requirements": [
                                    {
                                      "fieldKey": "BUSINESS_PLACE",
                                      "fieldLabel": "사업장 소재지",
                                      "fieldTypeCode": "SELECT",
                                      "scopeCode": "BUSINESS",
                                      "required": true,
                                      "sensitive": false,
                                      "sortOrder": 1,
                                      "helpText": "공고 입력 항목",
                                      "options": [
                                        {
                                          "optionCode": "ONLINE",
                                          "optionLabel": "온라인",
                                          "sortOrder": 1
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requirements[0].fieldTypeCode").value("SELECT"));
    }

    @Test
    void selectApplicationInputValuesReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(dynamicAnnouncementInputService.selectApplicationInputValues(any(), eq(PROGRESS_ID)))
                .thenReturn(inputValuesResponse());

        mockMvc.perform(get("/api/v1/application-progresses/{progressId}/input-values", PROGRESS_ID)
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.progressId").value(PROGRESS_ID.toString()))
                .andExpect(jsonPath("$.data.values[0].valueNumber").value(3000000));
    }

    @Test
    void saveApplicationInputValuesReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(dynamicAnnouncementInputService.saveApplicationInputValues(
                        any(),
                        eq(PROGRESS_ID),
                        any()
                ))
                .thenReturn(inputValuesResponse());

        mockMvc.perform(put("/api/v1/application-progresses/{progressId}/input-values", PROGRESS_ID)
                        .with(user(operatorPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "values": [
                                    {
                                      "requirementId": "%s",
                                      "valueNumber": 3000000
                                    }
                                  ]
                                }
                                """.formatted(REQUIREMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.values[0].fieldKey").value("BUSINESS_PLACE"));
    }

    private AnnouncementInputRequirementsResponse requirementsResponse() {
        return new AnnouncementInputRequirementsResponse(
                ANNOUNCEMENT_ID,
                List.of(new AnnouncementInputRequirementsResponse.RequirementResponse(
                        REQUIREMENT_ID,
                        "BUSINESS_PLACE",
                        "사업장 소재지",
                        "SELECT",
                        "BUSINESS",
                        true,
                        false,
                        1,
                        "공고 입력 항목",
                        List.of(new AnnouncementInputRequirementsResponse.OptionResponse(
                                OPTION_ID,
                                "ONLINE",
                                "온라인",
                                1
                        ))
                ))
        );
    }

    private ApplicationInputValuesResponse inputValuesResponse() {
        return new ApplicationInputValuesResponse(
                PROGRESS_ID,
                ANNOUNCEMENT_ID,
                List.of(new ApplicationInputValuesResponse.InputValueResponse(
                        REQUIREMENT_ID,
                        "BUSINESS_PLACE",
                        "사업장 소재지",
                        "AMOUNT",
                        "BUSINESS",
                        true,
                        false,
                        1,
                        "공고 입력 항목",
                        null,
                        new BigDecimal("3000000"),
                        null,
                        null,
                        null,
                        List.of(),
                        USER_ID,
                        OffsetDateTime.now()
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
}

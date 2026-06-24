/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: DynamicAnnouncementInputControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

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
import com.saneb.domain.dynamicinput.dto.StandardDocumentFieldResponse;
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
    private static final UUID STANDARD_FIELD_ID = UUID.fromString("80000000-0000-0000-0000-000000000005");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DynamicAnnouncementInputService dynamicAnnouncementInputService;

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 저장합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 저장합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectStandardDocumentFieldsReturnsConditionEligible() throws Exception {
        org.mockito.Mockito.when(dynamicAnnouncementInputService.selectStandardDocumentFieldList(null, null))
                .thenReturn(List.of(new StandardDocumentFieldResponse(
                        STANDARD_FIELD_ID,
                        "BUSINESS_REGISTRATION",
                        "OPENING_DATE",
                        "개업일",
                        "DATE",
                        "BUSINESS",
                        false,
                        true,
                        "CONDITION_READY",
                        10,
                        "사업자등록증에 표시된 개업일입니다."
                )));

        mockMvc.perform(get("/api/v1/standard-document-fields")
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].standardFieldId").value(STANDARD_FIELD_ID.toString()))
                .andExpect(jsonPath("$.data[0].conditionEligible").value(true))
                .andExpect(jsonPath("$.data[0].conditionUsageCode").value("CONDITION_READY"));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
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
                        null,
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
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

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
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

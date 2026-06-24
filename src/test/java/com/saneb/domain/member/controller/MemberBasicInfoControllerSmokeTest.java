/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: MemberBasicInfoControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.member.dto.MemberBasicInfoResponse;
import com.saneb.domain.member.service.MemberBasicInfoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class MemberBasicInfoControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberBasicInfoService memberBasicInfoService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(memberBasicInfoService.selectMyBasicInfo(any()))
                .thenReturn(sampleResponse());
        org.mockito.Mockito.when(memberBasicInfoService.saveMyBasicInfo(any(), any()))
                .thenReturn(sampleResponse());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectMyBasicInfoReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/member/basic-info")
                        .with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.roadAddress").value("서울특별시 중구 세종대로 110"))
                .andExpect(jsonPath("$.data.business.businessName").value("사내비상점"))
                .andExpect(jsonPath("$.data.business.workplaceRoadAddress").value("서울특별시 중구 세종대로 110"))
                .andExpect(jsonPath("$.data.business.employeeCount").value(5))
                .andExpect(jsonPath("$.data.business.niceCreditScore").value(750))
                .andExpect(jsonPath("$.data.business.hasExistingLoan").value(false))
                .andExpect(jsonPath("$.data.families[0].relationTypeCode").value("CHILD"))
                .andExpect(jsonPath("$.data.families[0].schoolAgeStatusCode").value("ELEMENTARY"))
                .andExpect(jsonPath("$.data.families[0].enrollmentStatusCode").value("ENROLLED"))
                .andExpect(jsonPath("$.data.interviewResponses[0].questionLabel").value("기존 동일 사업 진행 여부"))
                .andExpect(jsonPath("$.data.documentInputs[0].documentTypeLabel").value("사업자등록증"))
                .andExpect(jsonPath("$.data.documentInputs[0].fields[0].fieldLabel").value("사업장 주소"));
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void saveMyBasicInfoReturnsApiResponse() throws Exception {
        mockMvc.perform(put("/api/v1/member/basic-info")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 1988,
                                  "regionCode": "SEOUL",
                                  "postalCode": "04524",
                                  "roadAddress": "서울특별시 중구 세종대로 110",
                                  "jibunAddress": "서울특별시 중구 태평로1가 31",
                                  "detailAddress": "101호",
                                  "sidoName": "서울특별시",
                                  "sigunguName": "중구",
                                  "eupmyeondongName": "태평로1가",
                                  "legalDongCode": "1114010300",
                                  "roadNameCode": "111403005001",
                                  "buildingManagementNo": "1114010300100310000000001",
                                  "addressSourceCode": "JUSO_API",
                                  "incomePresenceCode": "HAS_INCOME",
                                  "incomeAmount": 30000000,
                                  "healthInsuranceBasisCode": "WORKPLACE",
                                  "business": {
                                    "representativeName": "홍길동",
                                    "businessRegistrationNo": "123-45-67890",
                                    "businessName": "사내비상점",
                                    "workplaceRegionCode": "SEOUL",
                                    "workplacePostalCode": "04524",
                                    "workplaceRoadAddress": "서울특별시 중구 세종대로 110",
                                    "workplaceJibunAddress": "서울특별시 중구 태평로1가 31",
                                    "workplaceDetailAddress": "2층",
                                    "workplaceSidoName": "서울특별시",
                                    "workplaceSigunguName": "중구",
                                    "workplaceEupmyeondongName": "태평로1가",
                                    "workplaceLegalDongCode": "1114010300",
                                    "workplaceRoadNameCode": "111403005001",
                                    "workplaceBuildingManagementNo": "1114010300100310000000001",
                                    "workplaceAddressSourceCode": "JUSO_API",
                                    "openingDate": "2022-01-01",
                                    "businessTypeCode": "SOLE_PROPRIETOR",
                                    "companyStageCode": "OPERATING",
                                    "annualRevenue": 120000000,
                                    "annualRevenueYear": 2025,
                                    "employeeCount": 5,
                                    "regularEmployeeCount": 3,
                                    "plannedHireCount": 1,
                                    "niceCreditScore": 750,
                                    "kcbCreditScore": 720,
                                    "hasExistingLoan": false,
                                    "hasPolicyFundUsage": false,
                                    "hasGuaranteeUsage": false
                                  },
                                  "families": [
                                    {
                                      "relationTypeCode": "CHILD",
                                      "birthYear": 2018,
                                      "schoolAgeStatusCode": "ELEMENTARY",
                                      "enrollmentStatusCode": "ENROLLED",
                                      "cohabiting": true,
                                      "supported": true,
                                      "incomePresenceCode": "NONE"
                                    }
                                  ],
                                  "interviewResponses": [
                                    {
                                      "questionCode": "SAME_BUSINESS_IN_PROGRESS",
                                      "answerCode": "NO"
                                    },
                                    {
                                      "questionCode": "OTHER_RESTRICTION",
                                      "answerCode": "UNKNOWN",
                                      "note": "확인 예정"
                                    }
                                  ],
                                  "documentInputs": [
                                    {
                                      "documentTypeCode": "BUSINESS_REGISTRATION",
                                      "fields": [
                                        {
                                          "standardFieldId": "10000000-0000-0000-0000-000000000010",
                                          "valueText": "서울특별시 중구"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.incomePresenceCode").value("HAS_INCOME"))
                .andExpect(jsonPath("$.data.documentInputs[0].selected").value(true));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void basicInfoApiRejectsOperatorRole() throws Exception {
        mockMvc.perform(get("/api/v1/member/basic-info")
                        .with(user(operatorPrincipal())))
                .andExpect(status().isForbidden());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private static MemberBasicInfoResponse sampleResponse() {
        return new MemberBasicInfoResponse(
                USER_ID,
                1988,
                "SEOUL",
                "04524",
                "서울특별시 중구 세종대로 110",
                "서울특별시 중구 태평로1가 31",
                "101호",
                "서울특별시",
                "중구",
                "태평로1가",
                "1114010300",
                "111403005001",
                "1114010300100310000000001",
                "JUSO_API",
                true,
                "HAS_INCOME",
                BigDecimal.valueOf(30_000_000),
                "WORKPLACE",
                new MemberBasicInfoResponse.BusinessInfoResponse(
                        "홍길동",
                        "123-45-67890",
                        "사내비상점",
                        "SEOUL",
                        "04524",
                        "서울특별시 중구 세종대로 110",
                        "서울특별시 중구 태평로1가 31",
                        "2층",
                        "서울특별시",
                        "중구",
                        "태평로1가",
                        "1114010300",
                        "111403005001",
                        "1114010300100310000000001",
                        "JUSO_API",
                        LocalDate.of(2022, 1, 1),
                        "47911",
                        "SOLE_PROPRIETOR",
                        "OPERATING",
                        BigDecimal.valueOf(120_000_000),
                        2025,
                        5,
                        3,
                        1,
                        750,
                        720,
                        false,
                        false,
                        false
                ),
                List.of(new MemberBasicInfoResponse.FamilyInfoResponse(
                        UUID.fromString("10000000-0000-0000-0000-000000000003"),
                        "CHILD",
                        2018,
                        "ELEMENTARY",
                        "ENROLLED",
                        true,
                        true,
                        false,
                        "NONE",
                        null
                )),
                List.of(
                        new MemberBasicInfoResponse.InterviewResponse(
                                "SAME_BUSINESS_IN_PROGRESS",
                                "기존 동일 사업 진행 여부",
                                "NO",
                                "아니오",
                                null
                        ),
                        new MemberBasicInfoResponse.InterviewResponse(
                                "OTHER_RESTRICTION",
                                "기타 제한 여부",
                                "UNKNOWN",
                                "잘 모르겠음",
                                "확인 예정"
                        )
                ),
                List.of(new MemberBasicInfoResponse.DocumentInputResponse(
                        "BUSINESS_REGISTRATION",
                        "사업자등록증",
                        true,
                        List.of(new MemberBasicInfoResponse.DocumentFieldInputResponse(
                                UUID.fromString("10000000-0000-0000-0000-000000000010"),
                                "WORKPLACE_ADDRESS",
                                "사업장 주소",
                                "TEXT",
                                "BUSINESS",
                                false,
                                45,
                                "사업자등록증에 표시된 사업장 주소입니다.",
                                "서울특별시 중구",
                                null,
                                null,
                                null
                        ))
                ))
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private static AuthenticatedUserDetails userPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(USER_ID, "user01", "{noop}pw", "사용자", "ACTIVE", false, null, null, null),
                List.of("USER")
        );
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private static AuthenticatedUserDetails operatorPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        UUID.fromString("10000000-0000-0000-0000-000000000004"),
                        "operator01",
                        "{noop}pw",
                        "운영자",
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

/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminMemberBasicInfoControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class AdminMemberBasicInfoControllerSmokeTest {

    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
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
        org.mockito.Mockito.when(memberBasicInfoService.selectMemberBasicInfo(any(), eq(USER_ID)))
                .thenReturn(sampleResponse());
        org.mockito.Mockito.when(memberBasicInfoService.saveMemberBasicInfo(any(), eq(USER_ID), any()))
                .thenReturn(sampleResponse());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectMemberBasicInfoReturnsApiResponseForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/member-basic-info/{userId}", USER_ID)
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.legalDongCode").value("1114010300"))
                .andExpect(jsonPath("$.data.business.kcbCreditScore").value(720))
                .andExpect(jsonPath("$.data.business.hasExistingLoan").value(false))
                .andExpect(jsonPath("$.data.interviewResponses[0].answerLabel").value("아니오"))
                .andExpect(jsonPath("$.data.documentInputs[0].documentTypeLabel").value("사업자등록증"));
    }

    /**
     * 업무 데이터를 저장합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void saveMemberBasicInfoReturnsApiResponseForAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/admin/member-basic-info/{userId}", USER_ID)
                        .with(user(adminPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 1988,
                                  "regionCode": "SEOUL",
                                  "postalCode": "04524",
                                  "roadAddress": "서울특별시 중구 세종대로 110",
                                  "legalDongCode": "1114010300",
                                  "addressSourceCode": "JUSO_API",
                                  "incomePresenceCode": "UNKNOWN",
                                  "families": [],
                                  "interviewResponses": [
                                    {
                                      "questionCode": "BUSINESS_ACTUALLY_OPERATING",
                                      "answerCode": "YES"
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
                .andExpect(jsonPath("$.data.documentInputs[0].selected").value(true));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectMemberBasicInfoReturnsApiResponseForOperator() throws Exception {
        mockMvc.perform(get("/api/v1/admin/member-basic-info/{userId}", USER_ID)
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void adminMemberBasicInfoApiRejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/member-basic-info/{userId}", USER_ID)
                        .with(user(userPrincipal())))
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
                List.of(),
                List.of(new MemberBasicInfoResponse.InterviewResponse(
                        "SAME_BUSINESS_IN_PROGRESS",
                        "기존 동일 사업 진행 여부",
                        "NO",
                        "아니오",
                        null
                )),
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
    private static AuthenticatedUserDetails adminPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(ADMIN_ID, "admin", "{noop}pw", "관리자", "ACTIVE", false, null, null, null),
                List.of("ADMIN")
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
                new AuthUserDetailsRow(ADMIN_ID, "operator", "{noop}pw", "운영자", "ACTIVE", false, null, null, null),
                List.of("OPERATOR")
        );
    }
}

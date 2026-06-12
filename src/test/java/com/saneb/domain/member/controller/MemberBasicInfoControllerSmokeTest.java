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

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(memberBasicInfoService.selectMyBasicInfo(any()))
                .thenReturn(sampleResponse());
        org.mockito.Mockito.when(memberBasicInfoService.saveMyBasicInfo(any(), any()))
                .thenReturn(sampleResponse());
    }

    @Test
    void selectMyBasicInfoReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/member/basic-info")
                        .with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.business.businessName").value("사내비상점"))
                .andExpect(jsonPath("$.data.families[0].relationTypeCode").value("CHILD"))
                .andExpect(jsonPath("$.data.documentInputs[0].documentTypeLabel").value("사업자등록증"))
                .andExpect(jsonPath("$.data.documentInputs[0].fields[0].fieldLabel").value("사업장 주소"));
    }

    @Test
    void saveMyBasicInfoReturnsApiResponse() throws Exception {
        mockMvc.perform(put("/api/v1/member/basic-info")
                        .with(user(userPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthYear": 1988,
                                  "regionCode": "SEOUL",
                                  "incomePresenceCode": "HAS_INCOME",
                                  "incomeAmount": 30000000,
                                  "healthInsuranceBasisCode": "WORKPLACE",
                                  "business": {
                                    "businessRegistrationNo": "123-45-67890",
                                    "businessName": "사내비상점",
                                    "workplaceRegionCode": "SEOUL",
                                    "openingDate": "2022-01-01",
                                    "businessTypeCode": "SOLE_PROPRIETOR",
                                    "companyStageCode": "OPERATING",
                                    "annualRevenue": 120000000,
                                    "annualRevenueYear": 2025,
                                    "hasPolicyFundUsage": false,
                                    "hasGuaranteeUsage": false
                                  },
                                  "families": [
                                    {
                                      "relationTypeCode": "CHILD",
                                      "birthYear": 2018,
                                      "incomePresenceCode": "NONE"
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

    @Test
    void basicInfoApiRejectsOperatorRole() throws Exception {
        mockMvc.perform(get("/api/v1/member/basic-info")
                        .with(user(operatorPrincipal())))
                .andExpect(status().isForbidden());
    }

    private static MemberBasicInfoResponse sampleResponse() {
        return new MemberBasicInfoResponse(
                USER_ID,
                1988,
                "SEOUL",
                true,
                "HAS_INCOME",
                BigDecimal.valueOf(30_000_000),
                "WORKPLACE",
                new MemberBasicInfoResponse.BusinessInfoResponse(
                        "123-45-67890",
                        "사내비상점",
                        "SEOUL",
                        LocalDate.of(2022, 1, 1),
                        "47911",
                        "SOLE_PROPRIETOR",
                        "OPERATING",
                        BigDecimal.valueOf(120_000_000),
                        2025,
                        false,
                        false
                ),
                List.of(new MemberBasicInfoResponse.FamilyInfoResponse(
                        UUID.fromString("10000000-0000-0000-0000-000000000003"),
                        "CHILD",
                        2018,
                        false,
                        "NONE",
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

    private static AuthenticatedUserDetails userPrincipal() {
        return new AuthenticatedUserDetails(
                new AuthUserDetailsRow(USER_ID, "user01", "{noop}pw", "사용자", "ACTIVE", false, null, null, null),
                List.of("USER")
        );
    }

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

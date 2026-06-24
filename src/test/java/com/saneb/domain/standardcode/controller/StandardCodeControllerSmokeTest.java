/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: StandardCodeControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.standardcode.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.standardcode.dto.StandardCodeGroupResponse;
import com.saneb.domain.standardcode.dto.StandardCodeResponse;
import com.saneb.domain.standardcode.service.StandardCodeService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class StandardCodeControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GROUP_ID = UUID.fromString("21000000-0000-0000-0000-000000000001");
    private static final UUID CODE_ID = UUID.fromString("21000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StandardCodeService standardCodeService;

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectStandardCodeGroupListReturnsApiResponse() throws Exception {
        org.mockito.Mockito.when(standardCodeService.selectStandardCodeGroupList())
                .thenReturn(List.of(new StandardCodeGroupResponse(
                        GROUP_ID,
                        "KSIC_11",
                        "한국표준산업분류 제11차",
                        "통계청",
                        "https://kssc.kostat.go.kr",
                        "제11차",
                        true
                )));

        mockMvc.perform(get("/api/v1/standard-code-groups")
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].groupCode").value("KSIC_11"))
                .andExpect(jsonPath("$.data[0].groupName").value("한국표준산업분류 제11차"));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    void selectStandardCodeListReturnsPagedApiResponse() throws Exception {
        org.mockito.Mockito.when(standardCodeService.selectStandardCodeList(
                        eq("KSIC_11"),
                        eq("음식"),
                        eq(true),
                        eq(1),
                        eq(20)
                ))
                .thenReturn(PageResponse.of(List.of(new StandardCodeResponse(
                        CODE_ID,
                        "KSIC_11",
                        "한국표준산업분류 제11차",
                        "56111",
                        "한식 일반 음식점업",
                        "56",
                        5,
                        56111,
                        true
                )), 1, 20, 1));

        mockMvc.perform(get("/api/v1/standard-codes")
                        .param("groupCode", "KSIC_11")
                        .param("keyword", "음식")
                        .with(user(operatorPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].code").value("56111"))
                .andExpect(jsonPath("$.data.items[0].codeName").value("한식 일반 음식점업"))
                .andExpect(jsonPath("$.data.page").value(1));
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

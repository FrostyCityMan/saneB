/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminUserManagementViewControllerSmokeTest.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.adminuser.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.service.AdminUserManagementService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AdminUserManagementViewControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserManagementService adminUserManagementService;

    /**
     * 업무 처리를 수행합니다.
     */
    @BeforeEach
    void setUp() {
        when(adminUserManagementService.selectUserList(any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(AdminUserManagementControllerSmokeTest.sampleUser()), 1, 20, 1));
        when(adminUserManagementService.selectRoleList())
                .thenReturn(AdminUserManagementControllerSmokeTest.sampleRoles());
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAdminUserManagementPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/admin-users"))
                .andExpect(content().string(containsString("회원관리")))
                .andExpect(content().string(containsString("권한")))
                .andExpect(content().string(containsString("data-admin-users-app")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @Test
    /**
     * 업무 데이터를 조회합니다.
     *
     * @throws Exception 처리 중 예외가 발생한 경우
     */
    @WithMockUser(username = "user01", roles = "USER")
    void selectAdminUserManagementPageRejectsNonAdminUser() throws Exception {
        mockMvc.perform(get("/app/admin/users"))
                .andExpect(status().isForbidden());
    }
}

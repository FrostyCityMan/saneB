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

    @BeforeEach
    void setUp() {
        when(adminUserManagementService.selectUserList(any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(AdminUserManagementControllerSmokeTest.sampleUser()), 1, 20, 1));
        when(adminUserManagementService.selectRoleList())
                .thenReturn(AdminUserManagementControllerSmokeTest.sampleRoles());
    }

    @Test
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

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectAdminUserManagementPageRejectsNonAdminUser() throws Exception {
        mockMvc.perform(get("/app/admin/users"))
                .andExpect(status().isForbidden());
    }
}

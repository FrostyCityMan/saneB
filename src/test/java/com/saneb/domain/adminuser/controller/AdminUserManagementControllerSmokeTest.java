package com.saneb.domain.adminuser.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.dto.AdminRoleResponse;
import com.saneb.domain.adminuser.dto.AdminUserSummaryResponse;
import com.saneb.domain.adminuser.service.AdminUserManagementService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AdminUserManagementControllerSmokeTest {

    static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserManagementService adminUserManagementService;

    @BeforeEach
    void setUp() {
        when(adminUserManagementService.selectUserList(any(), any(), any(), eq(1), eq(20)))
                .thenReturn(PageResponse.of(List.of(sampleUser()), 1, 20, 1));
        when(adminUserManagementService.selectRoleList()).thenReturn(sampleRoles());
        when(adminUserManagementService.updateUserStatus(any(), eq(USER_ID), any())).thenReturn(sampleUser());
        when(adminUserManagementService.updateUserRoles(any(), eq(USER_ID), any())).thenReturn(sampleUser());
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectUserListReturnsPagedApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].loginId").value("user01"))
                .andExpect(jsonPath("$.data.items[0].roles[0]").value("USER"));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectRoleListReturnsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roleCode").value("USER"));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void updateUserStatusReturnsApiResponse() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void updateUserRolesReturnsApiResponse() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/{userId}/roles", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCodes": ["USER", "OPERATOR"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void adminUserApisRejectNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    static AdminUserSummaryResponse sampleUser() {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdminUserSummaryResponse(
                USER_ID,
                "user01",
                "사용자",
                "010-0000-0000",
                "user01@example.com",
                "ACTIVE",
                false,
                now,
                now,
                now,
                List.of("USER")
        );
    }

    static List<AdminRoleResponse> sampleRoles() {
        return List.of(
                new AdminRoleResponse("USER", "사용자", 10),
                new AdminRoleResponse("OPERATOR", "운영자", 30),
                new AdminRoleResponse("ADMIN", "관리자", 50)
        );
    }
}

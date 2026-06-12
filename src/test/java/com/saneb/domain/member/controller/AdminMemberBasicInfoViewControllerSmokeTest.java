package com.saneb.domain.member.controller;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AdminMemberBasicInfoViewControllerSmokeTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserManagementService adminUserManagementService;

    @BeforeEach
    void setUp() {
        when(adminUserManagementService.selectUserList(any(), any(), eq("USER"), eq(1), eq(12)))
                .thenReturn(PageResponse.of(List.of(sampleUser()), 1, 12, 1));
        when(adminUserManagementService.selectRoleList()).thenReturn(sampleRoles());
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void selectAdminMemberBasicInfoPageReturnsThymeleafView() throws Exception {
        mockMvc.perform(get("/app/admin/member-basic-info"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/admin-member-basic-info"))
                .andExpect(content().string(containsString("고객 정보 입력")))
                .andExpect(content().string(containsString("data-admin-member-basic-info-app")))
                .andExpect(content().string(containsString("서류별 선택 입력")))
                .andExpect(content().string(not(containsString("th:utext"))));
    }

    @Test
    @WithMockUser(username = "user01", roles = "USER")
    void selectAdminMemberBasicInfoPageRejectsNonAdminUser() throws Exception {
        mockMvc.perform(get("/app/admin/member-basic-info"))
                .andExpect(status().isForbidden());
    }

    private static AdminUserSummaryResponse sampleUser() {
        return new AdminUserSummaryResponse(
                USER_ID,
                "user01",
                "사용자",
                "010-0000-0000",
                "user01@example.com",
                "ACTIVE",
                false,
                OffsetDateTime.parse("2026-06-01T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-01T10:00:00+09:00"),
                OffsetDateTime.parse("2026-06-01T10:00:00+09:00"),
                List.of("USER")
        );
    }

    private static List<AdminRoleResponse> sampleRoles() {
        return List.of(
                new AdminRoleResponse("USER", "일반 사용자", 10),
                new AdminRoleResponse("ADMIN", "관리자", 90)
        );
    }
}

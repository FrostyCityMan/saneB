package com.saneb.domain.adminuser.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saneb.common.error.ApiException;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.dao.AdminUserManagementDao;
import com.saneb.domain.adminuser.dto.AdminUserRolesUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserStatusUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserSummaryResponse;
import com.saneb.domain.adminuser.vo.AdminUserSummaryRow;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceImplTest {

    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private AdminUserManagementDao adminUserManagementDao;

    private AdminUserManagementServiceImpl adminUserManagementService;

    @BeforeEach
    void setUp() {
        adminUserManagementService = new AdminUserManagementServiceImpl(adminUserManagementDao);
    }

    @Test
    void selectUserListMapsRoleCodes() {
        when(adminUserManagementDao.selectUserCount(any())).thenReturn(1L);
        when(adminUserManagementDao.selectUserList(any())).thenReturn(List.of(userRow(USER_ID, "USER,OPERATOR")));

        PageResponse<AdminUserSummaryResponse> response =
                adminUserManagementService.selectUserList(null, null, null, 1, 20);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items().getFirst().roles()).containsExactly("USER", "OPERATOR");
    }

    @Test
    void updateUserStatusRejectsSelfDisable() {
        when(adminUserManagementDao.selectUserDetails(ADMIN_ID)).thenReturn(userRow(ADMIN_ID, "ADMIN"));

        assertThatThrownBy(() -> adminUserManagementService.updateUserStatus(
                adminAuthentication(),
                ADMIN_ID,
                new AdminUserStatusUpdateRequest("DISABLED")
        )).isInstanceOf(ApiException.class)
                .hasMessageContaining("현재 로그인한 관리자 계정은 잠금, 사용 중지, 삭제 처리할 수 없습니다.");

        verify(adminUserManagementDao, never()).updateUserStatus(any());
    }

    @Test
    void updateUserRolesRejectsSelfAdminRemoval() {
        when(adminUserManagementDao.selectUserDetails(ADMIN_ID)).thenReturn(userRow(ADMIN_ID, "ADMIN"));

        assertThatThrownBy(() -> adminUserManagementService.updateUserRoles(
                adminAuthentication(),
                ADMIN_ID,
                new AdminUserRolesUpdateRequest(List.of("USER"))
        )).isInstanceOf(ApiException.class)
                .hasMessageContaining("현재 로그인한 관리자 계정의 관리자 권한은 제거할 수 없습니다.");

        verify(adminUserManagementDao, never()).deleteUserRoles(any());
    }

    @Test
    void updateUserRolesReplacesRolesAndAudits() {
        when(adminUserManagementDao.selectUserDetails(USER_ID)).thenReturn(
                userRow(USER_ID, "USER"),
                userRow(USER_ID, "USER,OPERATOR")
        );

        AdminUserSummaryResponse response = adminUserManagementService.updateUserRoles(
                adminAuthentication(),
                USER_ID,
                new AdminUserRolesUpdateRequest(List.of("USER", "OPERATOR"))
        );

        assertThat(response.roles()).containsExactly("USER", "OPERATOR");
        verify(adminUserManagementDao).deleteUserRoles(USER_ID);
        verify(adminUserManagementDao).insertAuditLog(any());
    }

    private Authentication adminAuthentication() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                new AuthUserDetailsRow(
                        ADMIN_ID,
                        "admin",
                        "password-hash",
                        "관리자",
                        "ACTIVE",
                        false,
                        null,
                        null,
                        null
                ),
                List.of("ADMIN")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AdminUserSummaryRow userRow(UUID userId, String roleCodesText) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdminUserSummaryRow(
                userId,
                userId.equals(ADMIN_ID) ? "admin" : "user01",
                userId.equals(ADMIN_ID) ? "관리자" : "사용자",
                null,
                null,
                "ACTIVE",
                false,
                null,
                now,
                now,
                roleCodesText
        );
    }
}

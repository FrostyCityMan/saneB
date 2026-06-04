package com.saneb.domain.adminuser.dao;

import com.saneb.domain.adminuser.vo.AdminRoleRow;
import com.saneb.domain.adminuser.vo.AdminUserRoleCommand;
import com.saneb.domain.adminuser.vo.AdminUserSearchCondition;
import com.saneb.domain.adminuser.vo.AdminUserStatusCommand;
import com.saneb.domain.adminuser.vo.AdminUserSummaryRow;
import com.saneb.domain.adminuser.vo.AuditLogCommand;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserManagementDao {

    List<AdminUserSummaryRow> selectUserList(AdminUserSearchCondition condition);

    long selectUserCount(AdminUserSearchCondition condition);

    AdminUserSummaryRow selectUserDetails(@Param("userId") UUID userId);

    List<AdminRoleRow> selectRoleList();

    int updateUserStatus(AdminUserStatusCommand command);

    void deleteUserRoles(@Param("userId") UUID userId);

    void insertUserRole(AdminUserRoleCommand command);

    void insertAuditLog(AuditLogCommand command);
}

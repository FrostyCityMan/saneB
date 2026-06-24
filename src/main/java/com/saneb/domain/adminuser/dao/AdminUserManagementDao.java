/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminUserManagementDao.java
 * 작성자: 김도훈
 *
 */

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

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    List<AdminUserSummaryRow> selectUserList(AdminUserSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param condition 입력 값
     *
     * @return 처리 결과
     */
    long selectUserCount(AdminUserSearchCondition condition);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    AdminUserSummaryRow selectUserDetails(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    List<AdminRoleRow> selectRoleList();

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param command 입력 값
     *
     * @return 처리 결과
     */
    int updateUserStatus(AdminUserStatusCommand command);

    /**
     * 업무 데이터를 삭제합니다.
     *
     * @param userId 입력 값
     */
    void deleteUserRoles(@Param("userId") UUID userId);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertUserRole(AdminUserRoleCommand command);

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param command 입력 값
     */
    void insertAuditLog(AuditLogCommand command);
}

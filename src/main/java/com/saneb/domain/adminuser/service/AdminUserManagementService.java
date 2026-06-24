/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminUserManagementService.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.adminuser.service;

import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.dto.AdminRoleResponse;
import com.saneb.domain.adminuser.dto.AdminUserRolesUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserStatusUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AdminUserManagementService {

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param keyword 입력 값
     *
     * @param statusCode 입력 값
     *
     * @param roleCode 입력 값
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     *
     * @return 처리 결과
     */
    PageResponse<AdminUserSummaryResponse> selectUserList(
            String keyword,
            String statusCode,
            String roleCode,
            int page,
            int size
    );

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    List<AdminRoleResponse> selectRoleList();

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AdminUserSummaryResponse updateUserStatus(
            Authentication authentication,
            UUID userId,
            AdminUserStatusUpdateRequest request
    );

    /**
     * 업무 데이터를 수정합니다.
     *
     * @param authentication 입력 값
     *
     * @param userId 입력 값
     *
     * @param request 입력 값
     *
     * @return 처리 결과
     */
    AdminUserSummaryResponse updateUserRoles(
            Authentication authentication,
            UUID userId,
            AdminUserRolesUpdateRequest request
    );
}

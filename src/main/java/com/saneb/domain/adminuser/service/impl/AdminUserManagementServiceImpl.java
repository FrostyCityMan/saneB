/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: AdminUserManagementServiceImpl.java
 * 작성자: 김도훈
 *
 */

package com.saneb.domain.adminuser.service.impl;

import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.adminuser.dao.AdminUserManagementDao;
import com.saneb.domain.adminuser.dto.AdminRoleResponse;
import com.saneb.domain.adminuser.dto.AdminUserRolesUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserStatusUpdateRequest;
import com.saneb.domain.adminuser.dto.AdminUserSummaryResponse;
import com.saneb.domain.adminuser.service.AdminUserManagementService;
import com.saneb.domain.adminuser.vo.AdminRoleRow;
import com.saneb.domain.adminuser.vo.AdminUserRoleCommand;
import com.saneb.domain.adminuser.vo.AdminUserSearchCondition;
import com.saneb.domain.adminuser.vo.AdminUserStatusCommand;
import com.saneb.domain.adminuser.vo.AdminUserSummaryRow;
import com.saneb.domain.adminuser.vo.AuditLogCommand;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String RESOURCE_TYPE = "USER";
    private static final Set<String> STATUS_CODES = Set.of("ACTIVE", "LOCKED", "DISABLED", "DELETED");
    private static final Set<String> ROLE_CODES = Set.of("USER", "PARTNER", "OPERATOR", "APPROVER", "REVIEWER", "ADMIN");

    private final AdminUserManagementDao adminUserManagementDao;

    /**
     * 객체를 생성합니다.
     *
     * @param adminUserManagementDao 입력 값
     */
    public AdminUserManagementServiceImpl(AdminUserManagementDao adminUserManagementDao) {
        this.adminUserManagementDao = adminUserManagementDao;
    }

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
    @Override
    public PageResponse<AdminUserSummaryResponse> selectUserList(
            String keyword,
            String statusCode,
            String roleCode,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        String normalizedStatusCode = normalizeOptionalCode(statusCode);
        String normalizedRoleCode = normalizeOptionalCode(roleCode);
        validateOptionalCode("statusCode", normalizedStatusCode, STATUS_CODES);
        validateOptionalCode("roleCode", normalizedRoleCode, ROLE_CODES);

        AdminUserSearchCondition condition = new AdminUserSearchCondition(
                trimToNull(keyword),
                normalizedStatusCode,
                normalizedRoleCode,
                page,
                size,
                (page - 1) * size
        );
        long totalCount = adminUserManagementDao.selectUserCount(condition);
        List<AdminUserSummaryResponse> items = adminUserManagementDao.selectUserList(condition).stream()
                .map(this::toUserResponse)
                .toList();
        return PageResponse.of(items, page, size, totalCount);
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @return 처리 결과
     */
    @Override
    public List<AdminRoleResponse> selectRoleList() {
        return adminUserManagementDao.selectRoleList().stream()
                .map(this::toRoleResponse)
                .toList();
    }

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
    @Override
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
    @Transactional
    public AdminUserSummaryResponse updateUserStatus(
            Authentication authentication,
            UUID userId,
            AdminUserStatusUpdateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        AdminUserSummaryRow before = selectUserRow(userId);
        String statusCode = normalizeRequiredCode("statusCode", request.statusCode(), STATUS_CODES);
        if (actorUserId.equals(userId) && !"ACTIVE".equals(statusCode)) {
            throw new ApiException(
                    ErrorCode.AUTH_FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "현재 로그인한 관리자 계정은 잠금, 사용 중지, 삭제 처리할 수 없습니다."
            );
        }

        if (!statusCode.equals(before.statusCode())) {
            int updatedCount = adminUserManagementDao.updateUserStatus(new AdminUserStatusCommand(
                    userId,
                    statusCode,
                    actorUserId
            ));
            if (updatedCount == 0) {
                throw notFound();
            }
        }

        insertAudit(actorUserId, "USER_STATUS_UPDATE", userId, metadata(
                "beforeStatusCode", before.statusCode(),
                "afterStatusCode", statusCode,
                "changedCount", statusCode.equals(before.statusCode()) ? "0" : "1"
        ));
        return toUserResponse(selectUserRow(userId));
    }

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
    @Override
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
    @Transactional
    public AdminUserSummaryResponse updateUserRoles(
            Authentication authentication,
            UUID userId,
            AdminUserRolesUpdateRequest request
    ) {
        UUID actorUserId = selectRequiredActorUserId(authentication);
        selectUserRow(userId);
        List<String> roleCodes = normalizeRoleCodes(request.roleCodes());
        if (actorUserId.equals(userId) && !roleCodes.contains("ADMIN")) {
            throw new ApiException(
                    ErrorCode.AUTH_FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "현재 로그인한 관리자 계정의 관리자 권한은 제거할 수 없습니다."
            );
        }

        adminUserManagementDao.deleteUserRoles(userId);
        for (String roleCode : roleCodes) {
            adminUserManagementDao.insertUserRole(new AdminUserRoleCommand(
                    userId,
                    roleCode,
                    actorUserId
            ));
        }
        insertAudit(actorUserId, "USER_ROLES_UPDATE", userId, metadata(
                "roleCount", String.valueOf(roleCodes.size()),
                "adminIncluded", String.valueOf(roleCodes.contains("ADMIN")),
                "changedCount", "1"
        ));
        return toUserResponse(selectUserRow(userId));
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param userId 입력 값
     *
     * @return 처리 결과
     */
    private AdminUserSummaryRow selectUserRow(UUID userId) {
        AdminUserSummaryRow row = adminUserManagementDao.selectUserDetails(userId);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private AdminUserSummaryResponse toUserResponse(AdminUserSummaryRow row) {
        return new AdminUserSummaryResponse(
                row.userId(),
                row.loginId(),
                row.name(),
                row.phone(),
                row.email(),
                row.statusCode(),
                Boolean.TRUE.equals(row.passwordResetRequired()),
                row.lastLoginAt(),
                row.createdAt(),
                row.updatedAt(),
                splitRoleCodes(row.roleCodesText())
        );
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param row 입력 값
     *
     * @return 처리 결과
     */
    private AdminRoleResponse toRoleResponse(AdminRoleRow row) {
        return new AdminRoleResponse(row.roleCode(), row.roleName(), row.sortOrder());
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param roleCodes 입력 값
     *
     * @return 처리 결과
     */
    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        LinkedHashSet<String> normalizedRoleCodes = new LinkedHashSet<>();
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                normalizedRoleCodes.add(normalizeRequiredCode("roleCode", roleCode, ROLE_CODES));
            }
        }
        if (normalizedRoleCodes.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "권한을 하나 이상 선택하세요.");
        }
        return normalizedRoleCodes.stream()
                .sorted(Comparator.comparingInt(this::selectRolePriority))
                .toList();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param roleCodesText 입력 값
     *
     * @return 처리 결과
     */
    private List<String> splitRoleCodes(String roleCodesText) {
        if (roleCodesText == null || roleCodesText.isBlank()) {
            return List.of();
        }
        List<String> roleCodes = new ArrayList<>();
        for (String roleCode : roleCodesText.split(",")) {
            String normalizedRoleCode = roleCode.trim();
            if (!normalizedRoleCode.isEmpty()) {
                roleCodes.add(normalizedRoleCode);
            }
        }
        return roleCodes;
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param authentication 입력 값
     *
     * @return 처리 결과
     */
    private UUID selectRequiredActorUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserDetails principal) {
            return principal.userId();
        }
        throw new ApiException(
                ErrorCode.AUTH_REQUIRED,
                HttpStatus.UNAUTHORIZED,
                "DB 계정 인증이 필요합니다."
        );
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param page 입력 값
     *
     * @param size 입력 값
     */
    private void validatePageRequest(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ErrorCode.INVALID_PAGE_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "페이지 요청 값이 올바르지 않습니다."
            );
        }
    }

    /**
     * 요청 값과 업무 규칙을 검증합니다.
     *
     * @param fieldName 입력 값
     *
     * @param code 입력 값
     *
     * @param allowedCodes 입력 값
     */
    private void validateOptionalCode(String fieldName, String code, Set<String> allowedCodes) {
        if (code != null && !allowedCodes.contains(code)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 값이 올바르지 않습니다."
            );
        }
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param fieldName 입력 값
     *
     * @param code 입력 값
     *
     * @param allowedCodes 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeRequiredCode(String fieldName, String code, Set<String> allowedCodes) {
        String normalizedCode = normalizeOptionalCode(code);
        if (normalizedCode == null || !allowedCodes.contains(normalizedCode)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 값이 올바르지 않습니다."
            );
        }
        return normalizedCode;
    }

    /**
     * 입력 값을 표준 형식으로 정규화합니다.
     *
     * @param code 입력 값
     *
     * @return 처리 결과
     */
    private String normalizeOptionalCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase();
    }

    /**
     * 문자열 입력 값을 정리합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    private ApiException notFound() {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
    }

    /**
     * 업무 데이터를 등록합니다.
     *
     * @param actorUserId 입력 값
     *
     * @param actionCode 입력 값
     *
     * @param resourceId 입력 값
     *
     * @param metadataJson 입력 값
     */
    private void insertAudit(UUID actorUserId, String actionCode, UUID resourceId, String metadataJson) {
        adminUserManagementDao.insertAuditLog(new AuditLogCommand(
                actorUserId,
                actionCode,
                RESOURCE_TYPE,
                resourceId,
                "SUCCESS",
                metadataJson
        ));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param key1 입력 값
     *
     * @param value1 입력 값
     *
     * @param key2 입력 값
     *
     * @param value2 입력 값
     *
     * @param key3 입력 값
     *
     * @param value3 입력 값
     *
     * @return 처리 결과
     */
    private String metadata(String key1, String value1, String key2, String value2, String key3, String value3) {
        return "{\"" + key1 + "\":\"" + safeValue(value1) + "\",\""
                + key2 + "\":\"" + safeValue(value2) + "\",\""
                + key3 + "\":\"" + safeValue(value3) + "\"}";
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private String safeValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 업무 데이터를 조회합니다.
     *
     * @param roleCode 입력 값
     *
     * @return 처리 결과
     */
    private int selectRolePriority(String roleCode) {
        return switch (roleCode) {
            case "ADMIN" -> 1;
            case "APPROVER" -> 2;
            case "OPERATOR" -> 3;
            case "REVIEWER" -> 4;
            case "PARTNER" -> 5;
            default -> 6;
        };
    }
}

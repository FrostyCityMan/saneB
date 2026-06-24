/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ErrorCode.java
 * 작성자: 김도훈
 *
 */

package com.saneb.common.error;

public enum ErrorCode {
    AUTH_REQUIRED,
    AUTH_INVALID_CREDENTIALS,
    AUTH_FORBIDDEN,
    AUTH_PASSWORD_RESET_REQUIRED,
    CSRF_TOKEN_INVALID,
    VALIDATION_FAILED,
    INVALID_PAGE_REQUEST,
    INVALID_STATUS_TRANSITION,
    RESOURCE_NOT_FOUND,
    DUPLICATE_LOGIN_ID,
    DUPLICATE_PHONE,
    DUPLICATE_BUSINESS_REGISTRATION_NO,
    ANNOUNCEMENT_NOT_APPROVED,
    VERIFICATION_NOT_VERIFIED,
    MATCHING_BLOCKED,
    PROGRESS_STEP_LOCKED,
    PROGRESS_CONDITION_NOT_MET,
    RATE_LIMIT_EXCEEDED,
    DB_CONSTRAINT_VIOLATION,
    INTERNAL_ERROR
}

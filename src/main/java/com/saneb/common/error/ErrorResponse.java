/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ErrorResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.common.error;

import java.util.List;

public record ErrorResponse(
        ErrorCode errorCode,
        List<FieldErrorResponse> fieldErrors
) {

    /**
     * 업무 처리를 수행합니다.
     *
     * @param errorCode 입력 값
     *
     * @return 처리 결과
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode, List.of());
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param errorCode 입력 값
     *
     * @param fieldErrors 입력 값
     *
     * @return 처리 결과
     */
    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(errorCode, fieldErrors == null ? List.of() : fieldErrors);
    }

    public record FieldErrorResponse(
            String field,
            String message
    ) {
    }
}

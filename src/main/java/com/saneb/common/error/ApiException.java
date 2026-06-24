/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApiException.java
 * 작성자: 김도훈
 *
 */

package com.saneb.common.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    /**
     * 객체를 생성합니다.
     *
     * @param errorCode 입력 값
     *
     * @param httpStatus 입력 값
     *
     * @param message 입력 값
     */
    public ApiException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @return 처리 결과
     */
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}

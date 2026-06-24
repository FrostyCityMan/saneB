/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: ApiResponse.java
 * 작성자: 김도훈
 *
 */

package com.saneb.common.response;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {

    /**
     * 업무 처리를 수행합니다.
     *
     * @param data 입력 값
     *
     * @return 처리 결과
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "");
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param data 입력 값
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param data 입력 값
     *
     * @param message 입력 값
     *
     * @return 처리 결과
     */
    public static <T> ApiResponse<T> failure(T data, String message) {
        return new ApiResponse<>(false, data, message);
    }
}

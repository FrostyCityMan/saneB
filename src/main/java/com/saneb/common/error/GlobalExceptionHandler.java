/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: GlobalExceptionHandler.java
 * 작성자: 김도훈
 *
 */

package com.saneb.common.error;

import com.saneb.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param exception 입력 값
     *
     * @return 처리 결과
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleApiException(ApiException exception) {
        return ResponseEntity
                .status(exception.httpStatus())
                .body(ApiResponse.failure(ErrorResponse.of(exception.errorCode()), exception.getMessage()));
    }

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param exception 입력 값
     *
     * @return 처리 결과
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        List<ErrorResponse.FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorResponse)
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(
                        ErrorResponse.of(ErrorCode.VALIDATION_FAILED, fieldErrors),
                        "요청 값이 올바르지 않습니다."
                ));
    }

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param exception 입력 값
     *
     * @return 처리 결과
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        List<ErrorResponse.FieldErrorResponse> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ErrorResponse.FieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(
                        ErrorResponse.of(ErrorCode.VALIDATION_FAILED, fieldErrors),
                        "?붿껌 媛믪씠 ?щ컮瑜댁? ?딆뒿?덈떎."
                ));
    }

    /**
     * 업무 흐름을 처리합니다.
     *
     * @param exception 입력 값
     *
     * @return 처리 결과
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        ErrorResponse.of(ErrorCode.DB_CONSTRAINT_VIOLATION),
                        "?곗씠?곕쿋?댁뒪 ?쒖빟 議곌굔???꾨컲?섏뿀?듬땲??"
                ));
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param fieldError 입력 값
     *
     * @return 처리 결과
     */
    private ErrorResponse.FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new ErrorResponse.FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }
}

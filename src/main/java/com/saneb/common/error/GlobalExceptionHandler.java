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

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleApiException(ApiException exception) {
        return ResponseEntity
                .status(exception.httpStatus())
                .body(ApiResponse.failure(ErrorResponse.of(exception.errorCode()), exception.getMessage()));
    }

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

    private ErrorResponse.FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new ErrorResponse.FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }
}

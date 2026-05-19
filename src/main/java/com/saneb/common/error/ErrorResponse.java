package com.saneb.common.error;

import java.util.List;

public record ErrorResponse(
        ErrorCode errorCode,
        List<FieldErrorResponse> fieldErrors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(errorCode, fieldErrors == null ? List.of() : fieldErrors);
    }

    public record FieldErrorResponse(
            String field,
            String message
    ) {
    }
}

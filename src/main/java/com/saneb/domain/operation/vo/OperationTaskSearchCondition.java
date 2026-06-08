package com.saneb.domain.operation.vo;

public record OperationTaskSearchCondition(
        String taskTypeCode,
        String statusCode,
        String priorityCode,
        int page,
        int size,
        int offset
) {
}

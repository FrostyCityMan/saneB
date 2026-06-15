package com.saneb.domain.standardcode.vo;

public record StandardCodeSearchCondition(
        String groupCode,
        String keyword,
        Boolean active,
        int page,
        int size,
        int offset
) {
}

package com.saneb.domain.matching.vo;

public record MatchingMemberLookupSearchCondition(
        String keyword,
        int page,
        int size,
        int offset
) {
}

package com.saneb.domain.address.vo;

public record AddressSearchCondition(
        String keyword,
        int page,
        int size,
        String firstSort,
        boolean includeHistory
) {
}

package com.saneb.domain.announcementsource.provider.content;

import java.util.UUID;

/**
 * DB에 등록된 source와 provider가 반환한 공식 상세 URL을 함께 전달하는 내부 요청입니다.
 */
public record ProviderContentRequest(
        String providerCode,
        UUID registeredSourceId,
        String registeredSourceUrl,
        String officialDetailUrl
) {

    public ProviderContentRequest {
        if (providerCode == null || providerCode.isBlank()) {
            throw new IllegalArgumentException("providerCode is required");
        }
        if (registeredSourceId == null) {
            throw new IllegalArgumentException("registeredSourceId is required");
        }
        if (registeredSourceUrl == null || registeredSourceUrl.isBlank()) {
            throw new IllegalArgumentException("registeredSourceUrl is required");
        }
        if (officialDetailUrl == null || officialDetailUrl.isBlank()) {
            throw new IllegalArgumentException("officialDetailUrl is required");
        }
    }
}

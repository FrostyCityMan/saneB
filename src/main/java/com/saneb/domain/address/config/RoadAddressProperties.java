package com.saneb.domain.address.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "saneb.address.road")
public record RoadAddressProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        int timeoutMillis
) {
}

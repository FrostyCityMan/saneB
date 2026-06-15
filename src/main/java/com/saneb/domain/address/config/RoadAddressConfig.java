package com.saneb.domain.address.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RoadAddressProperties.class)
public class RoadAddressConfig {
}

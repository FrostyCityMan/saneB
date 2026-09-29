package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GyeongnamBoardAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGoseongProfileDetails() {
        return new GyeongnamBoardAttachmentDiscoveryProfile(GyeongnamBoardAttachmentDiscoveryProfile.Site.GOSEONG);
    }
    @Bean public AttachmentDiscoveryProfile selectChangwonProfileDetails() {
        return new LegacyFileResponseAttachmentDiscoveryProfile(
                new GyeongnamBoardAttachmentDiscoveryProfile(GyeongnamBoardAttachmentDiscoveryProfile.Site.CHANGWON), "LEGACY_BINARY_UTF8");
    }
}

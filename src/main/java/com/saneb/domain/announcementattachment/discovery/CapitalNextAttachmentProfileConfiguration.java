package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class CapitalNextAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYonginProfileDetails() { return new YonginGetAttachmentDiscoveryProfile(); }
    @Bean public AttachmentDiscoveryProfile selectUiwangProfileDetails() { return new UiwangPostAttachmentDiscoveryProfile(); }
}

package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UlsanAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectUlsanNamguProfileDetails() { return new UlsanNamguAttachmentDiscoveryProfile(); }
    @Bean public AttachmentDiscoveryProfile selectUlsanJungguProfileDetails() { return new UlsanJungguPostAttachmentDiscoveryProfile(); }
    @Bean public AttachmentDiscoveryProfile selectUlsanBukguProfileDetails() { return new UlsanBukguGetAttachmentDiscoveryProfile(); }
}

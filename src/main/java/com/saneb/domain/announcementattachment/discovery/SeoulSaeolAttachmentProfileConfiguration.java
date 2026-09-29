package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SeoulSaeolAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectEunpyeongProfileDetails() { return new EunpyeongAttachmentDiscoveryProfile(); }
    @Bean public AttachmentDiscoveryProfile selectSeochoProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_SEOCHO_GET_V1", "LGS-000023", "eminwon.seocho.go.kr", "SAFE_SAEOL_EMINWON", "th", false);
    }
}

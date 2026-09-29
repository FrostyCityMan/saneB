package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MetroSaeolAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectDaeguNamguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_DAEGU_NAMGU_GET_V1", "LGS-000048", "eminwon.nam.daegu.kr", "SAFE_SAEOL_EMINWON", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectDaeguBukguProfileDetails() { return new DaeguBukguPostAttachmentDiscoveryProfile(); }
}

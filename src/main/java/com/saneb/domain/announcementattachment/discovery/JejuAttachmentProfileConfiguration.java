package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JejuAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectJejusiProfileDetails() { return new JejusiAttachmentDiscoveryProfile(); }
    @Bean public AttachmentDiscoveryProfile selectSeogwipoProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_SEOGWIPO_GET_V1", "LGS-000244", "eminwon.seogwipo.go.kr", "SAFE_SAEOL_EMINWON", "td", false);
    }
}

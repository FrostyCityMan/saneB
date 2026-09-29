package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공식 상세에서 확인한 포항을 기존 포털 새올 엔진에 연결한다. */
@Configuration(proxyBeanMethods = false)
public class PohangAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectPohangProfileDetails() {
        return new PohangAttachmentDiscoveryProfile();
    }
}

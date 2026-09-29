package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MetroSecondAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGwangjuDongguProfileDetails() { return new GwangjuSaeolAttachmentDiscoveryProfile(GwangjuSaeolAttachmentDiscoveryProfile.Layout.DONGGU); }
    @Bean public AttachmentDiscoveryProfile selectGwangjuBukguProfileDetails() { return new GwangjuSaeolAttachmentDiscoveryProfile(GwangjuSaeolAttachmentDiscoveryProfile.Layout.BUKGU); }
    @Bean public AttachmentDiscoveryProfile selectDaejeonDongguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_DAEJEON_DONGGU_GET_V1", "LGS-000072", "eminwon.donggu.go.kr", "SAFE_SAEOL_EMINWON", "td", false);
    }
}

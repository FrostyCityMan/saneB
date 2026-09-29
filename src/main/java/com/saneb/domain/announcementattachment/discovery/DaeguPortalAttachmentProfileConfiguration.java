package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공식 상세에서 확인한 대구 동구를 기존 포털 새올 엔진에 연결한다. */
@Configuration(proxyBeanMethods = false)
public class DaeguPortalAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectDaeguDongguProfileDetails() {
        return new GyeongbukPortalAttachmentDiscoveryProfile("DAEGU_DONGGU", "LGS-000046", "SAEOL_GOSI",
                "dong.daegu.kr", "mid", "0201020000", false, true);
    }
}

package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공식 상세에서 확인한 오산시를 기존 포털 새올 엔진에 연결한다. */
@Configuration(proxyBeanMethods = false)
public class OsanAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectOsanProfileDetails() {
        return new GyeongbukPortalAttachmentDiscoveryProfile("OSAN", "LGS-000104", "SAEOL_GOSI",
                "osan.go.kr", "mId", "0302010000", false, false);
    }
}

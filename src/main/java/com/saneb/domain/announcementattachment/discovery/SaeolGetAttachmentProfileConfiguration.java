package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 운영/관리자 입력으로 host를 확장하지 않는다. 추가 기관은 실측 후 코드와 해당 hash로 배포한다. */
@Configuration(proxyBeanMethods = false)
public class SaeolGetAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectBusanNamguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_BUSAN_NAMGU_GET_V1", "LGS-000034", "eminwon.bsnamgu.go.kr",
                "SAFE_SAEOL_EMINWON_LEGACY", "th", true);
    }
    @Bean public AttachmentDiscoveryProfile selectDaeguDalseongProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_DAEGU_DALSEONG_GET_V1", "LGS-000052", "eminwon.dalseong.daegu.kr",
                "SAFE_SAEOL_EMINWON", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectDaeguJungguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_DAEGU_JUNGGU_GET_V1", "LGS-000045", "eminwon.jung.daegu.kr",
                "SAFE_SAEOL_EMINWON_LEGACY", "div.tal", false);
    }
    @Bean public AttachmentDiscoveryProfile selectHamanProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_HAMAN_GET_V1", "LGS-000233", "eminwon.haman.go.kr",
                "SAFE_SAEOL_EMINWON_CELL", "td", false);
    }
}

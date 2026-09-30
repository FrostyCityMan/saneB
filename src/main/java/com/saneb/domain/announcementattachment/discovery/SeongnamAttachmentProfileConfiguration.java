package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 성남의 공개 새올 GET 다운로드를 기존 검증 엔진에 고정 결합한다. */
@Configuration(proxyBeanMethods=false)
public class SeongnamAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectSeongnamProfileDetails(){
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_SEONGNAM_GET_V1","LGS-000089","eminwon.seongnam.go.kr","SAFE_SAEOL_EMINWON","th",false);
    }
}

package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공식 목록·상세에서 확인한 충청권 기관만 시스템 프로필로 연결한다. */
@Configuration(proxyBeanMethods=false)
public class ChungcheongAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectEumseongProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_EUMSEONG_GET_V1","LGS-000145","eminwon.eumseong.go.kr","SAFE_SAEOL_EMINWON_CELL","th",false);
    }
    @Bean public AttachmentDiscoveryProfile selectNonsanProfileDetails() { return new NonsanAttachmentDiscoveryProfile(); }
    @Bean public AttachmentDiscoveryProfile selectDangjinProfileDetails() {
        return new LegacyFormSaeolAttachmentDiscoveryProfile("LOCAL_DANGJIN_GET_V1","LGS-000155","eminwon.dangjin.go.kr","form","98%");
    }
    @Bean public AttachmentDiscoveryProfile selectCheongyangProfileDetails() {
        return new LegacyFormSaeolAttachmentDiscoveryProfile("LOCAL_CHEONGYANG_GET_V1","LGS-000159","eminwon.cheongyang.go.kr","form","98%");
    }
}

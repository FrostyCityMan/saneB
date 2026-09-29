package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 증평·단양 공식 상세의 구형 form과 첨부 GET 구조를 고정한다. */
@Configuration(proxyBeanMethods=false)
public class ChungbukLegacyAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectJeungpyeongProfileDetails() {
        return new LegacyFormSaeolAttachmentDiscoveryProfile("LOCAL_JEUNGPYEONG_GET_V1","LGS-000142","eminwon.jp.go.kr","form1","100%");
    }
    @Bean public AttachmentDiscoveryProfile selectDanyangProfileDetails() {
        return new LegacyFormSaeolAttachmentDiscoveryProfile("LOCAL_DANYANG_GET_V1","LGS-000146","eminwon.danyang.go.kr","form","98%");
    }
}

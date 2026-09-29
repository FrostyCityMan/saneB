package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class ChungcheongNextAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectJincheonProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_JINCHEON_GET_V1","LGS-000143","eminwon.jincheon.go.kr","SAFE_SAEOL_EMINWON","th",false);
    }
    @Bean public AttachmentDiscoveryProfile selectTaeanProfileDetails() { return new TaeanPostAttachmentDiscoveryProfile(); }
}

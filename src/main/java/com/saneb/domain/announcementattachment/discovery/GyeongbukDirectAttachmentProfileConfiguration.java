package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class GyeongbukDirectAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYeongdeokProfileDetails(){return new GyeongbukDirectAttachmentDiscoveryProfile(GyeongbukDirectAttachmentDiscoveryProfile.Site.YEONGDEOK);}
    @Bean public AttachmentDiscoveryProfile selectUljinProfileDetails(){return new GyeongbukDirectAttachmentDiscoveryProfile(GyeongbukDirectAttachmentDiscoveryProfile.Site.ULJIN);}
}

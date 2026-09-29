package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class GyeongbukCountyAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectCheongsongProfileDetails(){return new GyeongbukCountyAttachmentDiscoveryProfile(GyeongbukCountyAttachmentDiscoveryProfile.Site.CHEONGSONG);}
    @Bean public AttachmentDiscoveryProfile selectYeongyangProfileDetails(){return new GyeongbukCountyAttachmentDiscoveryProfile(GyeongbukCountyAttachmentDiscoveryProfile.Site.YEONGYANG);}
    @Bean public AttachmentDiscoveryProfile selectUlleungProfileDetails(){return new GyeongbukCountyAttachmentDiscoveryProfile(GyeongbukCountyAttachmentDiscoveryProfile.Site.ULLEUNG);}
}

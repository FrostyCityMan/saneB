package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class GangwonNextAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectDonghaeProfileDetails() { return new GangwonNextGetAttachmentDiscoveryProfile(GangwonNextGetAttachmentDiscoveryProfile.Site.DONGHAE); }
    @Bean public AttachmentDiscoveryProfile selectJeongseonProfileDetails() { return new GangwonNextGetAttachmentDiscoveryProfile(GangwonNextGetAttachmentDiscoveryProfile.Site.JEONGSEON); }
    @Bean public AttachmentDiscoveryProfile selectGangwonGoseongProfileDetails() { return new GangwonNextGetAttachmentDiscoveryProfile(GangwonNextGetAttachmentDiscoveryProfile.Site.GW_GOSEONG); }
    @Bean public AttachmentDiscoveryProfile selectYangyangProfileDetails() { return new YangyangPostAttachmentDiscoveryProfile(); }
}

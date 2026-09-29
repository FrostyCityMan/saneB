package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class GoryeongJinjuAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGoryeongProfileDetails(){return new GoryeongJinjuAttachmentDiscoveryProfile(GoryeongJinjuAttachmentDiscoveryProfile.Site.GORYEONG);}
    @Bean public AttachmentDiscoveryProfile selectJinjuProfileDetails(){return new GoryeongJinjuAttachmentDiscoveryProfile(GoryeongJinjuAttachmentDiscoveryProfile.Site.JINJU);}
}

package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.CapitalFifthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class CapitalFifthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectAnseongProfileDetails() { return new CapitalFifthAttachmentDiscoveryProfile(Site.ANSEONG); }
    @Bean public AttachmentDiscoveryProfile selectUijeongbuProfileDetails() { return new CapitalFifthAttachmentDiscoveryProfile(Site.UIJEONGBU); }
    @Bean public AttachmentDiscoveryProfile selectGgGwangjuProfileDetails() { return new CapitalFifthAttachmentDiscoveryProfile(Site.GG_GWANGJU); }
}

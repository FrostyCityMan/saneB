package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.CapitalSeventhNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class CapitalSeventhAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectPocheonProfileDetails() { return new CapitalSeventhAttachmentDiscoveryProfile(Site.POCHEON); }
    @Bean public AttachmentDiscoveryProfile selectGangneungProfileDetails() { return new CapitalSeventhAttachmentDiscoveryProfile(Site.GANGNEUNG); }
}

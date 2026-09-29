package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.IncheonPortalNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class IncheonPortalAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGyeyangProfileDetails(){return new IncheonPortalAttachmentDiscoveryProfile(Site.GYEYANG);}
    @Bean public AttachmentDiscoveryProfile selectGanghwaProfileDetails(){return new IncheonPortalAttachmentDiscoveryProfile(Site.GANGHWA);}
}

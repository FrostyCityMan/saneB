package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.CapitalThirdNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class CapitalThirdAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectNamyangjuProfileDetails(){return new CapitalThirdAttachmentDiscoveryProfile(Site.NAMYANGJU);}
    @Bean public AttachmentDiscoveryProfile selectHanamProfileDetails(){return new CapitalThirdAttachmentDiscoveryProfile(Site.HANAM);}
    @Bean public AttachmentDiscoveryProfile selectGuriProfileDetails(){return new CapitalThirdAttachmentDiscoveryProfile(Site.GURI);}
}

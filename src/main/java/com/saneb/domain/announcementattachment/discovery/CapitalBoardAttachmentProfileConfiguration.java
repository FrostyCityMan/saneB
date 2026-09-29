package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.CapitalEminwonNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class CapitalBoardAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYangjuProfileDetails(){return new CapitalBoardAttachmentDiscoveryProfile(Site.YANGJU);}
    @Bean public AttachmentDiscoveryProfile selectGunpoProfileDetails(){return new CapitalBoardAttachmentDiscoveryProfile(Site.GUNPO);}
    @Bean public AttachmentDiscoveryProfile selectYeojuProfileDetails(){return new CapitalBoardAttachmentDiscoveryProfile(Site.YEOJU);}
}

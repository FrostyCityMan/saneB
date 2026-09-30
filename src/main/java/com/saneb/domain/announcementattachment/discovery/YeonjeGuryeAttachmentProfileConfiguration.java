package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.saneb.domain.announcementsource.provider.content.YeonjeGuryeNoticePage.Site;

@Configuration(proxyBeanMethods=false)
public class YeonjeGuryeAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYeonjeProfileDetails(){return new YeonjeGuryeAttachmentDiscoveryProfile(Site.YEONJE);}
    @Bean public AttachmentDiscoveryProfile selectGuryeProfileDetails(){return new YeonjeGuryeAttachmentDiscoveryProfile(Site.GURYE);}
}

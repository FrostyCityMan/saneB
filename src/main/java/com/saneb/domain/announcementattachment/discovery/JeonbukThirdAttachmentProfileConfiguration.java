package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.JeonbukThirdNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class JeonbukThirdAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectJeonjuProfileDetails(){return new JeonbukThirdAttachmentDiscoveryProfile(Site.JEONJU);}
    @Bean public AttachmentDiscoveryProfile selectJeonbukProfileDetails(){return new JeonbukThirdAttachmentDiscoveryProfile(Site.JEONBUK);}
}

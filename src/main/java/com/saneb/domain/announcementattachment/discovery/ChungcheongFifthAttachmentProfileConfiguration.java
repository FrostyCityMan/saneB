package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.ChungcheongFifthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class ChungcheongFifthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGeumsanProfileDetails(){
        return new GeumsanFileResponseAttachmentProfile();
    }
    @Bean public AttachmentDiscoveryProfile selectBuyeoProfileDetails(){return new ChungcheongFifthAttachmentDiscoveryProfile(Site.BUYEO);}
}

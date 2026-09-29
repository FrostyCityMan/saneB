package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.ChungcheongFourthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class ChungcheongFourthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectHongseongProfileDetails(){return new ChungcheongFourthAttachmentDiscoveryProfile(Site.HONGSEONG);}
    @Bean public AttachmentDiscoveryProfile selectYesanProfileDetails(){return new ChungcheongFourthAttachmentDiscoveryProfile(Site.YESAN);}
}

package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.ChungcheongSixthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class ChungcheongSixthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectAsanProfileDetails(){return new ChungcheongSixthAttachmentDiscoveryProfile(Site.ASAN);}
    @Bean public AttachmentDiscoveryProfile selectSeosanProfileDetails(){return new ChungcheongSixthAttachmentDiscoveryProfile(Site.SEOSAN);}
}

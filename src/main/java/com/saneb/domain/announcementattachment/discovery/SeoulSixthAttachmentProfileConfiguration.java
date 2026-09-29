package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.SeoulSixthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class SeoulSixthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYangcheonProfileDetails(){return new SeoulSixthAttachmentDiscoveryProfile(Site.YANGCHEON);}
    @Bean public AttachmentDiscoveryProfile selectGwanakProfileDetails(){return new SeoulSixthAttachmentDiscoveryProfile(Site.GWANAK);}
}

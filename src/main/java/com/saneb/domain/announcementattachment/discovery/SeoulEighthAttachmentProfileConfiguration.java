package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.SeoulEighthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class SeoulEighthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGangnamProfileDetails(){return new SeoulEighthAttachmentDiscoveryProfile(Site.GANGNAM);}
    @Bean public AttachmentDiscoveryProfile selectDobongProfileDetails(){return new DobongUtf8AttachmentDiscoveryProfile(new SeoulEighthAttachmentDiscoveryProfile(Site.DOBONG));}
}

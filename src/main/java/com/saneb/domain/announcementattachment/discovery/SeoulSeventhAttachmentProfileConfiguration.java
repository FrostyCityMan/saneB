package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.SeoulSeventhNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class SeoulSeventhAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectSeoulProfileDetails(){return new SeoulSeventhAttachmentDiscoveryProfile(Site.SEOUL);}
    @Bean public AttachmentDiscoveryProfile selectSeoulJungguProfileDetails(){return new SeoulSeventhAttachmentDiscoveryProfile(Site.SEOUL_JUNGGU);}
    @Bean public AttachmentDiscoveryProfile selectYongsanProfileDetails(){
        return new LegacyFileResponseAttachmentDiscoveryProfile(new SeoulSeventhAttachmentDiscoveryProfile(Site.YONGSAN), "LEGACY_BINARY_UTF8");
    }
}

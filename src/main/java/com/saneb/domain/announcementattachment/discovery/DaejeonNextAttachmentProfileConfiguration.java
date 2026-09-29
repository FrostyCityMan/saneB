package com.saneb.domain.announcementattachment.discovery;
import com.saneb.domain.announcementsource.provider.content.DaejeonNextNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods=false)
public class DaejeonNextAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYuseongProfileDetails(){return new DaejeonNextAttachmentDiscoveryProfile(Site.YUSEONG);}
    @Bean public AttachmentDiscoveryProfile selectDaedeokProfileDetails(){return new DaejeonNextAttachmentDiscoveryProfile(Site.DAEDEOK);}
}

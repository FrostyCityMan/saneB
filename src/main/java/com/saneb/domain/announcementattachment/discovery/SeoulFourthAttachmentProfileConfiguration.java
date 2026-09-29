package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.SeoulFourthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class SeoulFourthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectSeongdongProfileDetails(){return new SeoulFourthAttachmentDiscoveryProfile(Site.SEONGDONG);}
    @Bean public AttachmentDiscoveryProfile selectSongpaProfileDetails(){return new SeoulFourthAttachmentDiscoveryProfile(Site.SONGPA);}
    @Bean public AttachmentDiscoveryProfile selectGwangjinProfileDetails(){return new SeoulFourthAttachmentDiscoveryProfile(Site.GWANGJIN);}
}

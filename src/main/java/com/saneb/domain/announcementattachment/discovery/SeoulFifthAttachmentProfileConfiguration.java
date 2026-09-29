package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.SeoulFifthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class SeoulFifthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectDongdaemunProfileDetails(){return new SeoulFifthAttachmentDiscoveryProfile(Site.DONGDAEMUN);}
    @Bean public AttachmentDiscoveryProfile selectSeongbukProfileDetails(){return new SeoulFifthAttachmentDiscoveryProfile(Site.SEONGBUK);}
    @Bean public AttachmentDiscoveryProfile selectYeongdeungpoProfileDetails(){return new SeoulFifthAttachmentDiscoveryProfile(Site.YEONGDEUNGPO);}
}

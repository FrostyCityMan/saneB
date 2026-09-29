package com.saneb.domain.announcementattachment.discovery;
import com.saneb.domain.announcementsource.provider.content.MetroNextNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods=false)
public class MetroNextAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGwangjuNamguProfileDetails(){return new MetroNextAttachmentDiscoveryProfile(Site.GWANGJU_NAMGU);}
    @Bean public AttachmentDiscoveryProfile selectDaejeonJungguProfileDetails(){return new MetroNextAttachmentDiscoveryProfile(Site.DAEJEON_JUNGGU);}
}

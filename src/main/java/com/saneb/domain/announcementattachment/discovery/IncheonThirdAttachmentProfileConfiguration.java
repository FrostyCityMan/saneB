package com.saneb.domain.announcementattachment.discovery;
import com.saneb.domain.announcementsource.provider.content.IncheonThirdNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods=false)
public class IncheonThirdAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGeomdanProfileDetails(){return new Utf8DispositionAttachmentDiscoveryProfile(new IncheonThirdAttachmentDiscoveryProfile(Site.GEOMDAN));}
    @Bean public AttachmentDiscoveryProfile selectYeongjongProfileDetails(){return new IncheonThirdAttachmentDiscoveryProfile(Site.YEONGJONG);}
}

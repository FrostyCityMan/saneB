package com.saneb.domain.announcementattachment.discovery;
import com.saneb.domain.announcementsource.provider.content.CapitalFourthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods=false)
public class CapitalFourthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGimpoProfileDetails(){return new CapitalFourthAttachmentDiscoveryProfile(Site.GIMPO);}
    @Bean public AttachmentDiscoveryProfile selectDongducheonProfileDetails(){return new CapitalFourthAttachmentDiscoveryProfile(Site.DONGDUCHEON);}
    @Bean public AttachmentDiscoveryProfile selectPyeongtaekProfileDetails(){return new CapitalFourthAttachmentDiscoveryProfile(Site.PYEONGTAEK);}
}

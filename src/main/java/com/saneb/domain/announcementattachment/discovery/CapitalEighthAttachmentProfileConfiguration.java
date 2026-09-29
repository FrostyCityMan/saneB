package com.saneb.domain.announcementattachment.discovery;
import com.saneb.domain.announcementsource.provider.content.CapitalEighthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods=false)
public class CapitalEighthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectPajuProfileDetails(){return new Utf8DispositionAttachmentDiscoveryProfile(new CapitalEighthAttachmentDiscoveryProfile(Site.PAJU));}
    @Bean public AttachmentDiscoveryProfile selectGwangmyeongProfileDetails(){return new CapitalEighthAttachmentDiscoveryProfile(Site.GWANGMYEONG);}
}

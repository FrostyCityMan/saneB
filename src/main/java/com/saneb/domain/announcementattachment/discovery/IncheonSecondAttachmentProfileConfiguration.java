package com.saneb.domain.announcementattachment.discovery;
import com.saneb.domain.announcementsource.provider.content.IncheonSecondNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods=false)
public class IncheonSecondAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectJemulpoProfileDetails(){return new Utf8DispositionAttachmentDiscoveryProfile(new IncheonSecondAttachmentDiscoveryProfile(Site.JEMULPO));}
    @Bean public AttachmentDiscoveryProfile selectMichuholProfileDetails(){return new IncheonSecondAttachmentDiscoveryProfile(Site.MICHUHOL);}
}

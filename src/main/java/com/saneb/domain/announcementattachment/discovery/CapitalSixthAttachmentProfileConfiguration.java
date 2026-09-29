package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.CapitalSixthNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class CapitalSixthAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectSiheungProfileDetails() { return new CapitalSixthAttachmentDiscoveryProfile(Site.SIHEUNG); }
    @Bean public AttachmentDiscoveryProfile selectAnsanProfileDetails() {
        return new ObservedBinaryMimeAttachmentDiscoveryProfile(
                new Utf8DispositionAttachmentDiscoveryProfile(new CapitalSixthAttachmentDiscoveryProfile(Site.ANSAN)),
                "application/unknown");
    }
}

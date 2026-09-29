package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.GangwonSecondNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class GangwonSecondAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYangguProfileDetails(){return new GangwonSecondAttachmentDiscoveryProfile(Site.YANGGU);}
    @Bean public AttachmentDiscoveryProfile selectInjeProfileDetails(){
        return new ObservedBinaryMimeAttachmentDiscoveryProfile(new GangwonSecondAttachmentDiscoveryProfile(Site.INJE),
                "application/x-tika-msoffice");
    }
}

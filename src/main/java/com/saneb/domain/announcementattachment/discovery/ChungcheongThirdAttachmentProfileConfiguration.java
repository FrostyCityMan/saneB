package com.saneb.domain.announcementattachment.discovery;

import com.saneb.domain.announcementsource.provider.content.ChungcheongThirdNoticePage.Site;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class ChungcheongThirdAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectChungbukProfileDetails(){return new ChungcheongThirdAttachmentDiscoveryProfile(Site.CHUNGBUK);}
    @Bean public AttachmentDiscoveryProfile selectGongjuProfileDetails(){return new ChungcheongThirdAttachmentDiscoveryProfile(Site.GONGJU);}
}

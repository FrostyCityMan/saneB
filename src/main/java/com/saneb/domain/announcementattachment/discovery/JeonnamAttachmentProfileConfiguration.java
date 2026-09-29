package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 실측한 공식 게시판과 공개 파일 서버를 시스템에서 고정한다. */
@Configuration(proxyBeanMethods=false)
public class JeonnamAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectMokpoProfileDetails(){return new JeonnamNoticeAttachmentDiscoveryProfile(JeonnamNoticeAttachmentDiscoveryProfile.Site.MOKPO);}
    @Bean public AttachmentDiscoveryProfile selectYeosuProfileDetails(){return new JeonnamNoticeAttachmentDiscoveryProfile(JeonnamNoticeAttachmentDiscoveryProfile.Site.YEOSU);}
    @Bean public AttachmentDiscoveryProfile selectNajuProfileDetails(){return new JeonnamNoticeAttachmentDiscoveryProfile(JeonnamNoticeAttachmentDiscoveryProfile.Site.NAJU);}
    @Bean public AttachmentDiscoveryProfile selectGangjinProfileDetails(){return new JeonnamNoticeAttachmentDiscoveryProfile(JeonnamNoticeAttachmentDiscoveryProfile.Site.GANGJIN);}
    @Bean public AttachmentDiscoveryProfile selectMuanProfileDetails(){return new JeonnamNoticeAttachmentDiscoveryProfile(JeonnamNoticeAttachmentDiscoveryProfile.Site.MUAN);}
}

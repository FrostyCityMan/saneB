package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공식 게시판 표본에서 확인한 경주·경산·의성 연결만 등록한다. */
@Configuration(proxyBeanMethods=false)
public class GyeongbukBoardAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGyeongjuProfileDetails(){return new GyeongbukBoardAttachmentDiscoveryProfile(GyeongbukBoardAttachmentDiscoveryProfile.Site.GYEONGJU);}
    @Bean public AttachmentDiscoveryProfile selectGyeongsanProfileDetails(){return new GyeongbukBoardAttachmentDiscoveryProfile(GyeongbukBoardAttachmentDiscoveryProfile.Site.GYEONGSAN);}
    @Bean public AttachmentDiscoveryProfile selectUiseongProfileDetails(){return new GyeongbukBoardAttachmentDiscoveryProfile(GyeongbukBoardAttachmentDiscoveryProfile.Site.UISEONG);}
}

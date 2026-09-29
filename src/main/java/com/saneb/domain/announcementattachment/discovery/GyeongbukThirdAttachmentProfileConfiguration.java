package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공식 상세 화면에서 확인한 영주·성주·예천의 지역별 첨부 연결. */
@Configuration(proxyBeanMethods=false)
public class GyeongbukThirdAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectYeongjuProfileDetails(){return new GyeongbukThirdAttachmentDiscoveryProfile(GyeongbukThirdAttachmentDiscoveryProfile.Site.YEONGJU);}
    @Bean public AttachmentDiscoveryProfile selectSeongjuProfileDetails(){return new GyeongbukThirdAttachmentDiscoveryProfile(GyeongbukThirdAttachmentDiscoveryProfile.Site.SEONGJU);}
    @Bean public AttachmentDiscoveryProfile selectYecheonProfileDetails(){return new GyeongbukThirdAttachmentDiscoveryProfile(GyeongbukThirdAttachmentDiscoveryProfile.Site.YECHEON);}
}

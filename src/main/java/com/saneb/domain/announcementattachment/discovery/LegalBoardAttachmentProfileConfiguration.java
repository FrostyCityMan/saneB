package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** source 코드와 실측 주소는 시스템 코드로만 배포한다. 관리자 parser 선택 기능이 아니다. */
@Configuration(proxyBeanMethods = false)
public class LegalBoardAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectBusanLegalProfileDetails() { return new LegalBoardAttachmentDiscoveryProfile(true); }
    @Bean public AttachmentDiscoveryProfile selectGangbukLegalProfileDetails() { return new LegalBoardAttachmentDiscoveryProfile(false); }
}

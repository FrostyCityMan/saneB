package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class GyeongnamNextAttachmentProfileConfiguration {
    // 두 기관의 실제 x-msdownload 응답·파일 서명 관측에 한해 기존 MIME/파일명 호환기를 적용한다.
    @Bean public AttachmentDiscoveryProfile selectGimhaeProfileDetails(){return new LegacyFileResponseAttachmentDiscoveryProfile(new GyeongnamNextAttachmentDiscoveryProfile("GIMHAE","gimhae.go.kr","LGS-000228","SCMS_CARD_NOTICE","/03360/00023/00029.web"),"LEGACY_BINARY_UTF8");}
    @Bean public AttachmentDiscoveryProfile selectChangnyeongProfileDetails(){return new LegacyFileResponseAttachmentDiscoveryProfile(new GyeongnamNextAttachmentDiscoveryProfile("CHANGNYEONG","cng.go.kr","LGS-000234","HEURISTIC_NOTICE","/03517/01553.web"),"LEGACY_BINARY_UTF8");}
}

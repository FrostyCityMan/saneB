package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 기관별 공식 게시판 실측에 따른 고정 등록. 관리자 selector/host 입력은 받지 않는다. */
@Configuration(proxyBeanMethods = false)
public class StandardBbsAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectTaebaekProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_TAEBAEK_BBS_V1", "LGS-000121", "www.taebaek.go.kr", "25", "352", false, true);
    }
    @Bean public AttachmentDiscoveryProfile selectHoengseongProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_HOENGSEONG_BBS_V1", "LGS-000125", "www.hsg.go.kr", "65", "821", true, false);
    }
    @Bean public AttachmentDiscoveryProfile selectYeongwolProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_YEONGWOL_BBS_V1", "LGS-000126", "www.yw.go.kr", "17", "273", false, false);
    }
}

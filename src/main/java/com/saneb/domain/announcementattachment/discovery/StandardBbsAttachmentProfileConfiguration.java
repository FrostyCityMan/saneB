package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 기관별 공식 게시판 실측에 따른 고정 등록. 관리자 selector/host 입력은 받지 않는다. */
@Configuration(proxyBeanMethods = false)
public class StandardBbsAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectTaebaekProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_TAEBAEK_BBS_V1", "LGS-000121", "www.taebaek.go.kr", "25", "352", StandardBbsAttachmentDiscoveryProfile.Layout.CLASSIC, true, "SPRING_BBS");
    }
    @Bean public AttachmentDiscoveryProfile selectHoengseongProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_HOENGSEONG_BBS_V1", "LGS-000125", "www.hsg.go.kr", "65", "821", StandardBbsAttachmentDiscoveryProfile.Layout.COMPACT, false, "SPRING_BBS");
    }
    @Bean public AttachmentDiscoveryProfile selectYeongwolProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_YEONGWOL_BBS_V1", "LGS-000126", "www.yw.go.kr", "17", "273", StandardBbsAttachmentDiscoveryProfile.Layout.CLASSIC, false, "SPRING_BBS");
    }
    @Bean public AttachmentDiscoveryProfile selectWonjuProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_WONJU_BBS_V1", "LGS-000118", "www.wonju.go.kr", "140", "216", StandardBbsAttachmentDiscoveryProfile.Layout.COMPACT_MENU_KEY, false, "HEURISTIC_NOTICE");
    }
    @Bean public AttachmentDiscoveryProfile selectJecheonProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_JECHEON_BBS_V1", "LGS-000138", "www.jecheon.go.kr", "18", "5233", StandardBbsAttachmentDiscoveryProfile.Layout.COMPACT_SVG, false, "HEURISTIC_NOTICE");
    }
    @Bean public AttachmentDiscoveryProfile selectBoeunProfileDetails() {
        return new StandardBbsAttachmentDiscoveryProfile("LOCAL_BOEUN_BBS_V1", "LGS-000139", "www.boeun.go.kr", "66", "194", StandardBbsAttachmentDiscoveryProfile.Layout.COMPACT_BOARD_PREVIEW, false, "HEURISTIC_NOTICE");
    }
}

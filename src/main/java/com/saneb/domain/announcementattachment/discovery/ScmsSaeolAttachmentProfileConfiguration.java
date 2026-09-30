package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ScmsSaeolAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGeochangProfileDetails() {
        return new ScmsSaeolAttachmentDiscoveryProfile("GEOCHANG", "geochang.go.kr", "LGS-000240", "SCMS_CARD_NOTICE",
                "/00445/00451.web", "/emwp/jsp/ofr/FileDownNew.jsp", true, false);
    }
    @Bean public AttachmentDiscoveryProfile selectHadongProfileDetails() {
        return new ScmsSaeolAttachmentDiscoveryProfile("HADONG", "hadong.go.kr", "LGS-000237", "SCMS_CARD_NOTICE",
                "/media/00012.web", "/emwp/jsp/ofr/FileDown.jsp", true, false);
    }
    @Bean public AttachmentDiscoveryProfile selectTongyeongProfileDetails() {
        return new TongyeongPeriodAttachmentDiscoveryProfile(new ScmsSaeolAttachmentDiscoveryProfile("TONGYEONG", "tongyeong.go.kr", "LGS-000226", "SAFE_SAEOL_EMINWON",
                "/00852/00853/00858.web", "/emwp/jsp/ofr/FileDownNewPbs.jsp", true, true));
    }
    @Bean public AttachmentDiscoveryProfile selectHapcheonProfileDetails() {
        // 기존 새올 원문과의 동등성은 타임아웃으로 미확인이다. 홈페이지 상세만 연결한다.
        return new ScmsSaeolAttachmentDiscoveryProfile("HAPCHEON", "hc.go.kr", "LGS-000241", "SAFE_SAEOL_EMINWON_CELL",
                "/04923/04924/04948.web", "/DownloadEx.do", false, false);
    }
}

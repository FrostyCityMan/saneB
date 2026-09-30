package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 운영/관리자 입력으로 host를 확장하지 않는다. 추가 기관은 실측 후 코드와 해당 hash로 배포한다. */
@Configuration(proxyBeanMethods = false)
public class SaeolGetAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGoesanProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_GOESAN_GET_V1", "LGS-000144", "eminwon.goesan.go.kr",
                "SAFE_SAEOL_EMINWON_CELL", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectHwasunProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_HWASUN_GET_V1", "LGS-000188", "eminwon.hwasun.go.kr",
                "SAFE_SAEOL_EMINWON", "td", false);
    }
    @Bean public AttachmentDiscoveryProfile selectSuseongProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_SUSEONG_GET_V1", "LGS-000050", "eminwon.suseong.kr",
                "SAFE_SAEOL_EMINWON", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectBusanJungguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_BUSAN_JUNGGU_GET_V1", "LGS-000028", "eminwon.bsjunggu.go.kr",
                "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectBusanSeoguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_BUSAN_SEOGU_GET_V1", "LGS-000029", "eminwon.bsseogu.go.kr",
                "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectBusanDongguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_BUSAN_DONGGU_GET_V1", "LGS-000030", "eminwon.bsdonggu.go.kr",
                "SAFE_SAEOL_EMINWON", "td", false);
    }
    @Bean public AttachmentDiscoveryProfile selectSahaProfileDetails() {
        return new SahaSaeolAttachmentDiscoveryProfile();
    }
    @Bean public AttachmentDiscoveryProfile selectBusanGangseoProfileDetails() {
        return new BusanGangseoAttachmentDiscoveryProfile();
    }
    @Bean public AttachmentDiscoveryProfile selectHaeundaeProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_HAEUNDAE_GET_V1", "LGS-000036", "eminwon.haeundae.go.kr",
                "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectGijangProfileDetails() {
        return new GijangSaeolAttachmentDiscoveryProfile();
    }
    @Bean public AttachmentDiscoveryProfile selectSuyeongProfileDetails() {
        return new BusanStructuredSaeolAttachmentDiscoveryProfile(BusanStructuredSaeolAttachmentDiscoveryProfile.Layout.SUYEONG);
    }
    @Bean public AttachmentDiscoveryProfile selectSasangProfileDetails() {
        return new BusanStructuredSaeolAttachmentDiscoveryProfile(BusanStructuredSaeolAttachmentDiscoveryProfile.Layout.SASANG);
    }
    @Bean public AttachmentDiscoveryProfile selectBusanjinProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_BUSANJIN_GET_V1", "LGS-000032", "eminwon.busanjin.go.kr",
                "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectGeumjeongProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_GEUMJEONG_GET_V1", "LGS-000038", "eminwon.geumjeong.go.kr",
                "SAFE_SAEOL_EMINWON_COMPACT", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectDongnaeProfileDetails() {
        return new DongnaeSaeolAttachmentDiscoveryProfile();
    }
    @Bean public AttachmentDiscoveryProfile selectBusanNamguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_BUSAN_NAMGU_GET_V1", "LGS-000034", "eminwon.bsnamgu.go.kr",
                "SAFE_SAEOL_EMINWON_LEGACY", "th", true);
    }
    @Bean public AttachmentDiscoveryProfile selectDaeguDalseongProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_DAEGU_DALSEONG_GET_V1", "LGS-000052", "eminwon.dalseong.daegu.kr",
                "SAFE_SAEOL_EMINWON", "th", false);
    }
    @Bean public AttachmentDiscoveryProfile selectDaeguJungguProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_DAEGU_JUNGGU_GET_V1", "LGS-000045", "eminwon.jung.daegu.kr",
                "SAFE_SAEOL_EMINWON_LEGACY", "div.tal", false);
    }
    @Bean public AttachmentDiscoveryProfile selectHamanProfileDetails() {
        return new SaeolGetAttachmentDiscoveryProfile("LOCAL_HAMAN_GET_V1", "LGS-000233", "eminwon.haman.go.kr",
                "SAFE_SAEOL_EMINWON_CELL", "td", false);
    }
}

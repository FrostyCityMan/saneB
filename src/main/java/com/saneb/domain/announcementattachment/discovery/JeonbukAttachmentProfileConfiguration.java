package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 공식 상세를 실측한 전북권 기관의 고정 결합. 운영 입력으로 호스트/파서를 확장하지 않는다. */
@Configuration(proxyBeanMethods=false)
public class JeonbukAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectGunsanProfileDetails(){return new SaeolGetAttachmentDiscoveryProfile("LOCAL_GUNSAN_GET_V1","LGS-000165","eminwon.gunsan.go.kr","SAFE_SAEOL_EMINWON","td",true);}
    @Bean public AttachmentDiscoveryProfile selectJeongeupProfileDetails(){return new SaeolGetAttachmentDiscoveryProfile("LOCAL_JEONGEUP_GET_V1","LGS-000167","eminwon.jeongeup.go.kr","SAFE_SAEOL_EMINWON","td",true);}
    @Bean public AttachmentDiscoveryProfile selectNamwonProfileDetails(){return new JeonbukBoardAttachmentDiscoveryProfile(JeonbukBoardAttachmentDiscoveryProfile.Site.NAMWON);}
    @Bean public AttachmentDiscoveryProfile selectBuanProfileDetails(){return new JeonbukBoardAttachmentDiscoveryProfile(JeonbukBoardAttachmentDiscoveryProfile.Site.BUAN);}
    @Bean public AttachmentDiscoveryProfile selectGochangProfileDetails(){return new JeonbukBoardAttachmentDiscoveryProfile(JeonbukBoardAttachmentDiscoveryProfile.Site.GOCHANG);}
    @Bean public AttachmentDiscoveryProfile selectIksanProfileDetails(){return new SaeolGetAttachmentDiscoveryProfile("LOCAL_IKSAN_GET_V1","LGS-000166","eminwon.iksan.go.kr","SAFE_SAEOL_EMINWON","td",false);}
    @Bean public AttachmentDiscoveryProfile selectWanjuProfileDetails(){return new FileboxSaeolAttachmentDiscoveryProfile("LOCAL_WANJU_GET_V1","LGS-000170","eminwon.wanju.go.kr");}
    @Bean public AttachmentDiscoveryProfile selectJinanProfileDetails(){return new SaeolGetAttachmentDiscoveryProfile("LOCAL_JINAN_GET_V1","LGS-000171","eminwon.jinan.go.kr","SAFE_SAEOL_EMINWON_CELL","td",false);}
    @Bean public AttachmentDiscoveryProfile selectMujuProfileDetails(){return new FileboxSaeolAttachmentDiscoveryProfile("LOCAL_MUJU_GET_V1","LGS-000172","eminwon.muju.go.kr");}
    @Bean public AttachmentDiscoveryProfile selectJangsuProfileDetails(){return new LegacyFormSaeolAttachmentDiscoveryProfile("LOCAL_JANGSU_GET_V1","LGS-000173","eminwon.jangsu.go.kr","form","98%");}
    @Bean public AttachmentDiscoveryProfile selectImsilProfileDetails(){return new SaeolGetAttachmentDiscoveryProfile("LOCAL_IMSIL_GET_V1","LGS-000174","eminwon.imsil.go.kr","SAFE_SAEOL_EMINWON","td",false);}
    @Bean public AttachmentDiscoveryProfile selectSunchangProfileDetails(){return new SaeolGetAttachmentDiscoveryProfile("LOCAL_SUNCHANG_GET_V1","LGS-000175","eminwon.sunchang.go.kr","SAFE_SAEOL_EMINWON","th",false);}
}

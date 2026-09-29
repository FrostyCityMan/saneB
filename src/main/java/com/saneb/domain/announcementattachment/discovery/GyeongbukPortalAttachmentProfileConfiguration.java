package com.saneb.domain.announcementattachment.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 실제 공식 포털에서 확인한 기관별 경로와 다운로드 방식을 고정한다. */
@Configuration(proxyBeanMethods=false)
public class GyeongbukPortalAttachmentProfileConfiguration {
    @Bean public AttachmentDiscoveryProfile selectCheongdoProfileDetails(){return new GyeongbukPortalAttachmentDiscoveryProfile("CHEONGDO","LGS-000215","SAEOL_GOSI","cheongdo.go.kr","mid","0301020000",false,true);}
    @Bean public AttachmentDiscoveryProfile selectChilgokProfileDetails(){return new GyeongbukPortalAttachmentDiscoveryProfile("CHILGOK","LGS-000218","SAEOL_GOSI","chilgok.go.kr","mId","0201030000",false,false);}
    @Bean public AttachmentDiscoveryProfile selectBonghwaProfileDetails(){return new GyeongbukPortalAttachmentDiscoveryProfile("BONGHWA","LGS-000220","SAEOL_GOSI","bonghwa.go.kr","mid","0201030000",false,true);}
    @Bean public AttachmentDiscoveryProfile selectGimcheonProfileDetails(){return new GyeongbukPortalAttachmentDiscoveryProfile("GIMCHEON","LGS-000203","SAEOL_GOSI","gc.go.kr","mId","1202180100",true,false);}
    @Bean public AttachmentDiscoveryProfile selectGumiProfileDetails(){return new GyeongbukPortalAttachmentDiscoveryProfile("GUMI","LGS-000205","SAEOL_GOSI","gumi.go.kr","mid","0401040000",false,true);}
    @Bean public AttachmentDiscoveryProfile selectYeongcheonProfileDetails(){return new GyeongbukPortalAttachmentDiscoveryProfile("YEONGCHEON","LGS-000207","YEONGCHEON_LEGAL_NOTICE","yc.go.kr","mId","0301040000",false,false);}
    @Bean public AttachmentDiscoveryProfile selectMungyeongProfileDetails(){return new GyeongbukPortalAttachmentDiscoveryProfile("MUNGYEONG","LGS-000209","SAEOL_GOSI","gbmg.go.kr","mId","0301060000",false,false);}
}

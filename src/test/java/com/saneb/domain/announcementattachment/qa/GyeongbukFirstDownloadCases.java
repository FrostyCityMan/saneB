package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongbukFirstDownloadCases {
    static final Set<String> GROUPS=Set.of("GIMCHEON","GUMI","YEONGCHEON","MUNGYEONG");
    static ObservationCase selectCase(String group){
        var config=new GyeongbukPortalAttachmentProfileConfiguration();
        AttachmentDiscoveryProfile p;String id,title,domain,menu;
        switch(group){
            case "GIMCHEON"->{p=config.selectGimcheonProfileDetails();id="41691";title="2026년 김천시 소상공인 온라인 마케팅 홍보비용 지원사업 보조사업자 모집 공고";domain="gc.go.kr";menu="mId=1202180100";}
            case "GUMI"->{p=config.selectGumiProfileDetails();id="65352";title="2025년 구미시 소상공인 카드수수료 지원사업 공고";domain="gumi.go.kr";menu="mid=0401040000";}
            case "YEONGCHEON"->{p=config.selectYeongcheonProfileDetails();id="36723";title="2024년 상반기 소상공인 정책자금 융자금이자 지원신청 공고";domain="yc.go.kr";menu="mId=0301040000";}
            case "MUNGYEONG"->{p=config.selectMungyeongProfileDetails();id="44422";title="2026년 문경시 소상공인 고효율기기 지원사업 지방보조금 지원 공고";domain="gbmg.go.kr";menu="mId=0301060000";}
            default->throw new IllegalArgumentException("GROUP_REQUIRED");
        }
        String base="https://www."+domain+"/portal/saeol/gosi/",url=base+"view.do?notAncmtMgtNo="+id+"&"+menu;
        var n=new AnnouncementSourceIdentityNormalizer();var binding=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,binding.localSourceCode(),binding.listParserProfileCode()),p,base+"list.do?"+menu,group.equals("MUNGYEONG")?2:1,group.equals("GUMI")?TitleLayout.GYEONGBUK_SUBJECT:TitleLayout.GYEONGBUK_HEADING);
    }
}

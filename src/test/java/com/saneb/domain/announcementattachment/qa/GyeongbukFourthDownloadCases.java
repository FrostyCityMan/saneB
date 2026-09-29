package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongbukFourthDownloadCases {
    static final Set<String> GROUPS=Set.of("CHEONGDO","CHILGOK","BONGHWA");
    static ObservationCase selectCase(String group){
        var config=new GyeongbukPortalAttachmentProfileConfiguration();
        AttachmentDiscoveryProfile p;String id,title,domain,menu;int count;
        switch(group){
            case "CHEONGDO"->{p=config.selectCheongdoProfileDetails();id="24118";title="지역 중소상공인 TV홈쇼핑 판로확보 지원사업 모집공고";domain="cheongdo.go.kr";menu="mid=0301020000";count=2;}
            case "CHILGOK"->{p=config.selectChilgokProfileDetails();id="34056";title="2026년 다자녀 가정 큰 집 마련 이자 지원사업(2차) 공고";domain="chilgok.go.kr";menu="mId=0201030000";count=1;}
            case "BONGHWA"->{p=config.selectBonghwaProfileDetails();id="32956";title="2026년 소상공인 고효율기기 지원사업 지원공고";domain="bonghwa.go.kr";menu="mid=0201030000";count=3;}
            default->throw new IllegalArgumentException("GROUP_REQUIRED");
        }
        String base="https://www."+domain+"/portal/saeol/gosi/",url=base+"view.do?notAncmtMgtNo="+id+"&"+menu;
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,base+"list.do?"+menu,count,group.equals("CHILGOK")?TitleLayout.GYEONGBUK_HEADING:TitleLayout.GYEONGBUK_SUBJECT);
    }
}

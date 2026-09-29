package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class SeoulFourthDownloadCases {
    static final Set<String> GROUPS=Set.of("SEONGDONG","SONGPA","GWANGJIN");
    static ObservationCase selectCase(String group){var c=new SeoulFourthAttachmentProfileConfiguration();AttachmentDiscoveryProfile p;String id,title,url,list;int count;TitleLayout layout;
        switch(group){
            case "SEONGDONG"->{p=c.selectSeongdongProfileDetails();id="356569";title="성동구 아동·청소년 체험학습카드 지원사업 가맹점 모집 공고";url="https://www.sd.go.kr/main/selectBbsNttView.do?bbsNo=184&nttNo=356569&key=1473";list="https://www.sd.go.kr/main/selectBbsNttList.do?bbsNo=184&key=1473&";count=2;layout=TitleLayout.SEONGDONG_BOARD;}
            case "SONGPA"->{p=c.selectSongpaProfileDetails();id="33174";title="2026년 송파구 중소기업 융자지원(협력자금) 계획 공고";url="https://www.songpa.go.kr/www/selectGosiData.do?not_ancmt_mgt_no=33174&key=2776";list="https://www.songpa.go.kr/www/selectGosiList.do?key=2776";count=3;layout=TitleLayout.SONGPA_BOARD;}
            case "GWANGJIN"->{p=c.selectGwangjinProfileDetails();id="6415244";title="2025년 하반기 소상공인 냉·난방기 클린케어 지원사업 모집 공고";url="https://www.gwangjin.go.kr/portal/bbs/B0000003/view.do?nttId=6415244&menuNo=200192";list="https://www.gwangjin.go.kr/portal/bbs/B0000003/list.do?menuNo=200192";count=3;layout=TitleLayout.GWANGJIN_BOARD;}
            default->throw new IllegalArgumentException("QA_GROUP_REQUIRED");
        }
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,count,layout);
    }
}

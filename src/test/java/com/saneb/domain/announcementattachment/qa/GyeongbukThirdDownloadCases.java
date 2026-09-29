package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongbukThirdDownloadCases {
    static final Set<String> GROUPS=Set.of("YEONGJU","SEONGJU","YECHEON");
    static ObservationCase selectCase(String group){
        var c=new GyeongbukThirdAttachmentProfileConfiguration();AttachmentDiscoveryProfile p;String id,title,url,list;int count;TitleLayout layout;
        switch(group){
            case "YEONGJU"->{p=c.selectYeongjuProfileDetails();id="24194";title="2026년 영주시 소상공인 고효율기기 지원사업 지방보조금 지원 공고";list="https://www.yeongju.go.kr/open_content/main/page.do?mnu_uid=10619";url=list+"&not_ancmt_mgt_no="+id+"&cmd=2";count=3;layout=TitleLayout.YEONGJU_BOARD;}
            case "SEONGJU"->{p=c.selectSeongjuProfileDetails();id="586507";title="2026년 청년농업인 자립기반 구축지원 사업공고";list="https://www.sj.go.kr/page.do?mnu_uid=1044";url=list+"&bod_uid="+id+"&cmd=258";count=2;layout=TitleLayout.SEONGJU_BOARD;}
            case "YECHEON"->{p=c.selectYecheonProfileDetails();id="30761";title="2026년 예천군 소상공인 특례보증 지원계획 공고";list="https://www.ycg.kr/open.content/ko/administrative/news/announcement/";url=list+"?id="+id;count=1;layout=TitleLayout.YECHEON_BOARD;}
            default->throw new IllegalArgumentException("GROUP_REQUIRED");
        }
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,count,layout);
    }
}

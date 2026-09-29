package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class SeoulSeventhDownloadCases {
    static final Set<String> GROUPS=Set.of("SEOUL","SEOUL_JUNGGU","YONGSAN");
    static ObservationCase selectCase(String group){var c=new SeoulSeventhAttachmentProfileConfiguration();AttachmentDiscoveryProfile p;String id,title,url,list;int count;TitleLayout layout;
        switch(group){
            case "SEOUL"->{p=c.selectSeoulProfileDetails();id="466130";title="2026년 서울시 청년 마음건강 지원사업 4차 참여자 모집 공고(추가모집)";url="https://www.seoul.go.kr/news/news_notice.do?bbsNo=277&nttNo=466130";list="https://www.seoul.go.kr/news/news_notice.do?bbsId=001&bbsNo=277";count=2;layout=TitleLayout.SEOUL_BOARD;}
            case "SEOUL_JUNGGU"->{p=c.selectSeoulJungguProfileDetails();id="1475799545";title="2026년 4/4분기 중소기업육성기금 융자지원 계획";url="https://www.junggu.seoul.kr/content.do?cmsid=14232&mode=view&cid=1475799545";list="https://www.junggu.seoul.kr/content.do?cmsid=14232&mode=list";count=3;layout=TitleLayout.SEOUL_JUNGGU_BOARD;}
            case "YONGSAN"->{p=c.selectYongsanProfileDetails();id="766830";title="2026년 용산구 일자리기금 청년기업 융자지원 확대 계획 공고";url="https://health.yongsan.go.kr/portal/bbs/B0000095/view.do?nttId=766830&menuNo=200233";list="https://health.yongsan.go.kr/portal/bbs/B0000095/list.do?menuNo=200233";count=3;layout=TitleLayout.YONGSAN_BOARD;}
            default->throw new IllegalArgumentException("QA_GROUP_REQUIRED");
        }
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,count,layout);
    }
}

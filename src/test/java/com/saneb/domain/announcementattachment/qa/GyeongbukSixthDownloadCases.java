package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongbukSixthDownloadCases {
    static final Set<String> GROUPS=Set.of("CHEONGSONG","YEONGYANG","ULLEUNG");
    static ObservationCase selectCase(String group){
        var c=new GyeongbukCountyAttachmentProfileConfiguration();AttachmentDiscoveryProfile p;String id,list,url,title;int count;TitleLayout layout;
        switch(group){
            case "CHEONGSONG"->{p=c.selectCheongsongProfileDetails();id="22287";list="https://www.cs.go.kr/news/00002679/00006203.web";url=list+"?amode=view&not_ancmt_mgt_no="+id+"&withPast=Y";title="2022년 청송군 소상공인 맞춤형 재난지원금 접수 안내";count=2;layout=TitleLayout.CHEONGSONG_BOARD;}
            case "YEONGYANG"->{p=c.selectYeongyangProfileDetails();id="26774";list="https://www.yyg.go.kr/www/organization/yyg_news/notification";url=list+"?idx="+id+"&mode=view";title="2026년 소상공인 고효율기기 지원사업 지방보조금 지원 공고";count=1;layout=TitleLayout.JEONNAM_NAJU_TITLE;}
            case "ULLEUNG"->{p=c.selectUlleungProfileDetails();id="23003";list="https://www.ulleung.go.kr/ko/page.do?mnu_uid=571&boardType=notice";url="https://www.ulleung.go.kr/ko/page.do?mnu_uid=571&not_ancmt_mgt_no="+id+"&cmd=2";title="2026년도 소상공인 고효율기기 지원사업  지방보조금 지원 공고";count=1;layout=TitleLayout.UISEONG_BOARD;}
            default->throw new IllegalArgumentException("GROUP_REQUIRED");
        }
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,count,layout);
    }
}

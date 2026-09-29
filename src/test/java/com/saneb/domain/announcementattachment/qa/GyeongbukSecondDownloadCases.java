package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class GyeongbukSecondDownloadCases {
    static final Set<String> GROUPS=Set.of("GYEONGJU","GYEONGSAN","UISEONG");
    static ObservationCase selectCase(String group){
        var c=new GyeongbukBoardAttachmentProfileConfiguration();AttachmentDiscoveryProfile p;String id,title,base,menu,url;
        switch(group){
            case "GYEONGJU"->{p=c.selectGyeongjuProfileDetails();id="240954";title="소상공인 정책자금(대리대출) 3분기 접수 공고 알림";base="https://www.gyeongju.go.kr/open_content/ko/page.do";menu="423";}
            case "GYEONGSAN"->{p=c.selectGyeongsanProfileDetails();id="250988";title="2026년 경산시 소상공인 특례보증·이차보전 지원 사업 공고";base="https://www.gbgs.go.kr/open_content/ko/page.do";menu="2160";}
            case "UISEONG"->{p=c.selectUiseongProfileDetails();id="39093";title="2026년 소상공인 고효율기기 지원사업 공고";base="https://www.usc.go.kr/ko/page.do";menu="157";}
            default->throw new IllegalArgumentException("GROUP_REQUIRED");
        }
        url=base+(group.equals("UISEONG")?"?mnu_uid="+menu+"&not_ancmt_mgt_no="+id+"&cmd=2":"?step=258&parm_bod_uid="+id+"&mnu_uid="+menu);
        var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,base+"?mnu_uid="+menu,group.equals("GYEONGJU")?3:1,group.equals("UISEONG")?TitleLayout.UISEONG_BOARD:TitleLayout.GYEONGBUK_BOARD);
    }
}

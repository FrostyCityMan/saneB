package com.saneb.domain.announcementattachment.qa;

import java.util.List;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class JeonnamFirstDownloadCases {
    static final List<String> GROUPS=List.of("MOKPO","YEOSU","NAJU","GANGJIN","MUAN");
    static ObservationCase selectCase(String group){
        var c=new JeonnamAttachmentProfileConfiguration();
        var p=switch(group){case "MOKPO"->c.selectMokpoProfileDetails();case "YEOSU"->c.selectYeosuProfileDetails();case "NAJU"->c.selectNajuProfileDetails();case "GANGJIN"->c.selectGangjinProfileDetails();case "MUAN"->c.selectMuanProfileDetails();default->throw new IllegalArgumentException("UNKNOWN_FIXED_GROUP");};
        String id=switch(group){case "MOKPO"->"54011";case "YEOSU"->"79153";case "NAJU"->"40288";case "GANGJIN"->"28097";default->"33617";};
        String title=switch(group){case "MOKPO"->"2026년 목포시 소상공인 융자금 이차보전 지원사업 정정공고";case "YEOSU"->"2026년도 여수시 소상공인 융자금 이차보전 지원 계획 변경 공고";case "NAJU"->"2026년도 나주시 소상공인 이차보전 지원사업 공고";case "GANGJIN"->"2026년 강진군 소상공인 융자금 이차보전 지원사업 시행 공고";default->"2026년 소상공인 디지털 전환 지원사업 신청·접수 안내";};
        String base=switch(group){case "MOKPO"->"https://www.mokpo.go.kr/www/mokpo_news/notification/public_notice";case "YEOSU"->"https://www.yeosu.go.kr/www/govt/news/notify/new_notifys/new_notify";case "NAJU"->"https://www.naju.go.kr/www/administration/notice/gosi_new";case "GANGJIN"->"https://www.gangjin.go.kr/www/government/notice/gosi";default->"https://www.muan.go.kr/www/openmuan/new/announcement";};
        String list=switch(group){case "MOKPO"->"https://www.mokpo.go.kr/www/mokpo_news/notification";case "YEOSU"->"https://www.yeosu.go.kr/www/govt/news/notify";default->base;};
        var layout=group.equals("NAJU")?TitleLayout.JEONNAM_NAJU_TITLE:group.equals("MUAN")?TitleLayout.JEONNAM_MUAN_TITLE:TitleLayout.JEONNAM_VIEW_TITLE;
        String url=base+"?idx="+id+"&mode=view";var n=new AnnouncementSourceIdentityNormalizer();var b=p.selectSourceBindings().getFirst();
        return new ObservationCase(group+"-"+id,title,new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,b.localSourceCode(),b.listParserProfileCode()),p,list,1,layout);
    }
}

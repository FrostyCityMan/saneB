package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class OngjinDownloadCases {
    static ObservationCase selectCase(){
        return selectCase(false);
    }
    static ObservationCase selectTitleExcludedCase(){return selectCase(true);}
    private static ObservationCase selectCase(boolean excluded){
        String id=excluded?"36423":"36422";
        String url="https://www.ongjin.go.kr/open_content/main/eminwon/eminwonAnnounceDetail.do?mgt_no="+id;
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("ONGJIN-"+id,excluded?"「2026년 옹진군 소상공인 경영환경개선사업」 계획 및 신청 안내 공고":"2026년 옹진군 소상공인 카드수수료 지원사업 계획 및 신청 안내 공고",
            new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000065","SPRING_BBS"),new OngjinAttachmentDiscoveryProfile(),
            "https://www.ongjin.go.kr/open_content/main/community/board/announce.jsp",1,TitleLayout.ONGJIN_PORTAL);
    }
}

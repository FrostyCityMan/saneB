package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class JeonnamThirdDownloadCases {
    static ObservationCase selectJangseongCase(){
        var p=new JeonnamAttachmentProfileConfiguration().selectJangseongProfileDetails();
        String url="https://www.jangseong.go.kr/home/www/news/jangseong/announcement/show/29332?page=1";
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("JANGSEONG-29332","2026년 하반기 장성군 소상공인 지원사업 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000196","SPRING_BBS"),p,"https://www.jangseong.go.kr/home/www/news/jangseong/announcement",1,TitleLayout.JANGSEONG_BOARD);
    }
}

package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class JangheungDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.jangheung.go.kr/www/organization/news/notification?idx=27081&mode=view";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("JANGHEUNG-27081","2026년 장흥군 소상공인 대출금 이차보전 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000189","SPRING_BBS"),
                new JangheungAttachmentDiscoveryProfile(),"https://www.jangheung.go.kr/www/organization/news/notification",1,TitleLayout.JANGHEUNG_BOARD);
    }
}

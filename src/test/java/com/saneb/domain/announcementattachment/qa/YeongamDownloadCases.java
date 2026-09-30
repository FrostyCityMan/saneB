package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class YeongamDownloadCases {
    static ObservationCase selectCase(){
        var profile=new YeongamAttachmentDiscoveryProfile();
        String url="https://www.yeongam.go.kr/home/www/open_information/yeongam_news/announcement/announcement_01/show/40370?page=1";
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("YEONGAM-40370","전남광주 청년 문화복지카드 지원사업 3차 모집 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000192","SPRING_BBS"),profile,
                "https://www.yeongam.go.kr/home/www/open_information/yeongam_news/announcement/yeongam.go",1,TitleLayout.YEONGAM_BOARD);
    }
}

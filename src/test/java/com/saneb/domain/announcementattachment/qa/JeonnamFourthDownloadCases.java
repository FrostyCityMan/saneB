package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class JeonnamFourthDownloadCases {
    static final Set<String> GROUPS=Set.of("BOSEONG");
    static ObservationCase selectCase(String group){
        if(!GROUPS.contains(group))throw new IllegalArgumentException("UNKNOWN_FIXED_GROUP");
        String list="https://www.boseong.go.kr/www/open_administration/city_news/notification",url=list+"?idx=37905&mode=view";var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("BOSEONG-37905","2026년 전남청년 문화복지카드 지원사업 3차 모집 공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000187","SPRING_BBS"),new BoseongAttachmentDiscoveryProfile(),list,1,TitleLayout.BOSEONG_BOARD);
    }
}

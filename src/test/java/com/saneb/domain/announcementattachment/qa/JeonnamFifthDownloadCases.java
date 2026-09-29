package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class JeonnamFifthDownloadCases {
    static final Set<String> GROUPS=Set.of("HAMPYEONG");
    static ObservationCase selectCase(String group){
        if(!GROUPS.contains(group))throw new IllegalArgumentException("UNKNOWN_FIXED_GROUP");
        String url="https://www.hampyeong.go.kr/pg/GosiDetail.do?SEQ=32368&pageId=www273&notAncmtSeCode=01,02,03,04";var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("HAMPYEONG-32368","「2026년 소상공인 카드수수료 지원사업」공고",new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000194","HEURISTIC_NOTICE"),new HampyeongAttachmentDiscoveryProfile(),"https://www.hampyeong.go.kr/pg/GosiList.do?pageId=www273",1,TitleLayout.HAMPYEONG_BOARD);
    }
}

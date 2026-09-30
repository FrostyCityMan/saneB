package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.GwangyangAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class GwangyangDownloadCases {
    static ObservationCase selectCase() {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        // 실제 목록 collector와 동일하게 canonical URL을 저장하고 그 URL의 hash를 식별자로 사용한다.
        String url = normalizer.canonicalizeUrl("https://gwangyang.go.kr/saeol/gosi.es?mid=a10909020000&act=view&type_code=02,04&seq=61869&nPage=1");
        return new ObservationCase("GWANGYANG-61869","2026년 광양시 소상공인 융자금 이차보전 계획 공고(2차)",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(url),url,"LGS-000182","SPRING_BBS"),
                new GwangyangAttachmentDiscoveryProfile(),"https://gwangyang.go.kr/menu.es?mid=a10909010000",1,TitleLayout.GWANGYANG_BOARD);
    }
}

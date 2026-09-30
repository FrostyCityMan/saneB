package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.CheongjuAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class CheongjuDownloadCases {
    static ObservationCase selectCase() {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = normalizer.canonicalizeUrl("https://www.cheongju.go.kr/www/selectEminwonNoticeView.do?key=281&nowDongGn=&notAncmtSeCd=&notAncmtMgtNo=150057");
        return new ObservationCase("CHEONGJU-150057","2026년도 청주형 소상공인 육성자금 지원 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(url),url,"LGS-000136","SAEOL_GOSI"),
                new CheongjuAttachmentDiscoveryProfile(),"https://www.cheongju.go.kr/www/selectEminwonNoticeList.do?key=281",1,TitleLayout.CHEONGJU_BOARD);
    }
}

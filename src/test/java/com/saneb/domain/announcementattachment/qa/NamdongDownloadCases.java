package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.NamdongAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class NamdongDownloadCases {
    static ObservationCase selectCase() {
        String url="https://www.namdong.go.kr/main/eminwon/eminwonAnnounceDetail.do?mgt_no=71702";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("NAMDONG-71702","『2026년 남동구 청년도전지원사업』 참여자 모집공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(url),url,"LGS-000059","HEURISTIC_NOTICE"),
                new NamdongAttachmentDiscoveryProfile(),"https://www.namdong.go.kr/main/news/announce.jsp",1,TitleLayout.NAMDONG_PORTAL);
    }
}

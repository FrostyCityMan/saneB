package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.SeoulGangseoAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class SeoulGangseoDownloadCases {
    static ObservationCase selectCase() {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = normalizer.canonicalizeUrl("https://www.gangseo.seoul.kr/gs040301/view?srchPage=&curPage=1&srchKey=&srchText=&mgtNo=66840");
        return new ObservationCase("SEOUL-GANGSEO-66840","강서구 미취업청년 자격증 응시료 지원 사업 신청자 모집 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(url),url,"LGS-000017","SAEOL_GOSI"),
                new SeoulGangseoAttachmentDiscoveryProfile(),"https://www.gangseo.seoul.kr/gs040301",2,TitleLayout.SEOUL_GANGSEO_BOARD);
    }
}

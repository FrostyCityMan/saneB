package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.UljuAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class UljuDownloadCases {
    static ObservationCase selectCase() {
        String url = "https://www.ulju.ulsan.kr/ulju/saeol/gosi/view.do?notAncmtMgtNo=67082&mId=0403020000";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("ULJU-67082","2026년 제2차 울주군 소상공인 자금 특례보증 지원 공고(수정)",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000082","SAEOL_GOSI"),
                new UljuAttachmentDiscoveryProfile(),"https://www.ulju.ulsan.kr/ulju/contents.do?mId=0403010000",1,TitleLayout.ULJU_BOARD);
    }
}

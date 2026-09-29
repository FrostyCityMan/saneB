package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GunwiDownloadCases {
    static ObservationCase selectCase() {
        var profile = new GunwiAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.gunwi.go.kr/ko/page.do?mnu_uid=666&not_ancmt_mgt_no=25554&cmd=2";
        return new ObservationCase("GUNWI-25554", "2026년도 군위군 소상공인 융자금 이차보전금 지원사업 운영계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000053", "GUNWI_NOTICE_TABLE"), profile,
                "https://www.gunwi.go.kr/ko/page.do?mnu_uid=666&boardType=notice", 1, TitleLayout.GUNWI_BOARD);
    }
}

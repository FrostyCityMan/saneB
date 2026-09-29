package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class HongcheonDownloadCases {
    static ObservationCase selectCase() {
        var profile = new HongcheonAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.hongcheon.go.kr/www/selectEminwonView.do?key=278&not_ancmt_mgt_no=53249";
        return new ObservationCase("HONGCHEON-53249", "홍천군 소상공인 특례보증(이차보전) 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000124", "SPRING_BBS"), profile,
                "https://www.hongcheon.go.kr/www/selectEminwonList.do?key=278", 1, TitleLayout.HONGCHEON_BOARD);
    }
}

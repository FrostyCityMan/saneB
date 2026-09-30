package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class ShinanDownloadCases {
    static ObservationCase selectCase() {
        String url = "https://www.shinan.go.kr/home/www/openinfo/participation_07/participation_07_04/show/38211?page=1";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("SHINAN-38211", "2026년도 전라남도 소상공인 육성자금 지원계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)), url,
                        "LGS-000199", "SPRING_BBS"), new ShinanAttachmentDiscoveryProfile(),
                "https://www.shinan.go.kr/home/www/openinfo/participation_07/participation_07_04/page.wscms", 1, TitleLayout.SHINAN_BOARD);
    }
}

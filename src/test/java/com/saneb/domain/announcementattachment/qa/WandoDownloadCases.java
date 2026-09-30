package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class WandoDownloadCases {
    static ObservationCase selectCase() {
        String url = "https://www.wando.go.kr/wando/sub.cs?m=1031&nttId=30322";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("WANDO-30322", "2026년 소상공인 디지털 전환 지원사업 신청ㆍ접수 안내",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)), url,
                        "LGS-000197", "SPRING_BBS"), new WandoAttachmentDiscoveryProfile(),
                "https://www.wando.go.kr/wando/sub.cs?m=318", 1, TitleLayout.WANDO_BOARD);
    }
}

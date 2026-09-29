package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class PyeongchangDownloadCases {
    static ObservationCase selectCase() {
        var profile = new PyeongchangAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String list = "https://www.pc.go.kr/portal/government/government-notification";
        String url = list + "?noticeMgrNo=41378";
        return new ObservationCase("PYEONGCHANG-41378", "평창군 소상공인 특례보증 지원사업 개정 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000127", "SPRING_BBS"), profile, list, 1, TitleLayout.PYEONGCHANG_BOARD);
    }
}

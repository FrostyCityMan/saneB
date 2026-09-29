package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class DaeguSeoguDownloadCases {
    static ObservationCase selectCase() {
        var profile = new DaeguSeoguAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.dgs.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=26070&mid=0601020200";
        return new ObservationCase("DAEGU_SEOGU-26070", "소기업.소상공인 방역물품비 지원금 시행 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000047", "SPRING_BBS"), profile,
                "https://www.dgs.go.kr/portal/contents.do?mid=0601020000", 1, TitleLayout.GYEONGBUK_SUBJECT);
    }
}

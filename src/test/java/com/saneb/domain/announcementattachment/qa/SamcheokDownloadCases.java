package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SamcheokDownloadCases {
    static ObservationCase selectCase() {
        var profile = new SamcheokAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String list = "https://www.samcheok.go.kr/media/00084/00095.web";
        String url = list + "?amode=view&mgtNo=36177&cd=01";
        return new ObservationCase("SAMCHEOK-36177", "2026년 소상공인 육성자금 융자추천 계획 공고(수정)",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000123", "SCMS_CARD_NOTICE"), profile, list, 1, TitleLayout.YEONGDO_HEADING);
    }
}

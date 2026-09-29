package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class OsanDownloadCases {
    static ObservationCase selectCase() {
        var profile = new OsanAttachmentProfileConfiguration().selectOsanProfileDetails();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.osan.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=50603&mId=0302010000";
        return new ObservationCase("OSAN-50603", "2026년 오산시 소상공인 특례보증 지원계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000104", "SAEOL_GOSI"), profile,
                "https://www.osan.go.kr/portal/saeol/gosi/list.do?mId=0302010000", 2, TitleLayout.GYEONGBUK_HEADING);
    }
}

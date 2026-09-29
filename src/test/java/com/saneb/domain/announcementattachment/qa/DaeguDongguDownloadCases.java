package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class DaeguDongguDownloadCases {
    static ObservationCase selectCase() {
        var profile = new DaeguPortalAttachmentProfileConfiguration().selectDaeguDongguProfileDetails();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.dong.daegu.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=60818&mid=0201020000";
        return new ObservationCase("DAEGU_DONGGU-60818", "2026년 하반기 대구광역시 동구 소상공인 경영안정자금 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000046", "SAEOL_GOSI"), profile,
                "https://www.dong.daegu.kr/portal/contents.do?mid=0201020000", 1, TitleLayout.GYEONGBUK_SUBJECT);
    }
}

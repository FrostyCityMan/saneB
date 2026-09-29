package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class PohangDownloadCases {
    static ObservationCase selectCase() {
        var profile = new PohangAttachmentProfileConfiguration().selectPohangProfileDetails();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.pohang.go.kr/portal/saeol/gosi/view.do?notAncmtMgtNo=73525&mid=0202010000";
        return new ObservationCase("POHANG-73525", "2026년 소상공인 고효율기기 지원사업 시행 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000201", "SAEOL_GOSI"), profile,
                "https://www.pohang.go.kr/portal/saeol/gosi/list.do?mid=0202010000", 1, TitleLayout.GYEONGBUK_SUBJECT);
    }
}

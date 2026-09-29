package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GeumcheonDownloadCases {
    static ObservationCase selectCase() {
        var profile = new GeumcheonAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = "https://www.geumcheon.go.kr/portal/tblSeolGosiDetailView.do?key=294&notAncmtMgtNo=27579";
        return new ObservationCase("GEUMCHEON-27579", "집합금지 및 영업제한 업종 소상공인 폐업지원금 변경 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000019", "SAEOL_GOSI"), profile,
                "https://www.geumcheon.go.kr/portal/tblSeolGosiDetailList.do?key=294&rep=1", 3, TitleLayout.GEUMCHEON_BOARD);
    }
}

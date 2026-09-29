package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class ChuncheonDownloadCases {
    static ObservationCase selectCase() {
        var profile = new ChuncheonAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String list = "https://www.chuncheon.go.kr/cityhall/administrative-info/notice-info/notice-announcement/";
        String url = list + "view/?notAncmtMgtNo=73071";
        return new ObservationCase("CHUNCHEON-73071","2026년 소상공인 경영환경개선 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,
                        "LGS-000117","CHUNCHEON_NOTICE_JSON"),profile,list,1,TitleLayout.CLASSIC_LABEL);
    }
}

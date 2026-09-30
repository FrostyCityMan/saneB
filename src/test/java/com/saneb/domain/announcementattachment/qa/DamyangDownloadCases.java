package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class DamyangDownloadCases {
    static ObservationCase selectCase() {
        var profile = new DamyangAttachmentDiscoveryProfile();
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String query = "domainId=DOM_0000001&contentsSid=2&menuCd=DOM_000000190001002001";
        String list = "https://www.damyang.go.kr/eminwon/searchList?" + query + "&boardType=special&listType=01";
        String url = "https://www.damyang.go.kr/eminwon/searchDetail?notAncmtMgtNo=37086&domainId=DOM_0000001&menuCd=DOM_000000190001002001&contentsSid=2&listType=01";
        return new ObservationCase("DAMYANG-37086","2026년 담양군 소상공인 야간경관 전기료 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,
                        "LGS-000183","DAMYANG_NOTICE_JSON"),profile,list,1,TitleLayout.CLASSIC_LABEL);
    }
}

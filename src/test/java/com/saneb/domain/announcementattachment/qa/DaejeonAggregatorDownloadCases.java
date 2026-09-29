package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class DaejeonAggregatorDownloadCases {
    static ObservationCase selectCase() {
        String url = "https://eminwon.seogu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?subCheck=Y&jndinm=OfrNotAncmtEJB&context=NTIS&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=49944";
        var n = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("DAEJEON_AGGREGATOR-49944", "2026년 대전 서구 소상공인 경영환경개선 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url, "LGS-000071", "DAEJEON_EMINWON_AGGREGATOR"),
                new DaejeonAggregatorAttachmentDiscoveryProfile(), "https://www.daejeon.go.kr/drh/MediaList.do?notiType=NOTI_06&menuSeq=2564", 1, TitleLayout.DAEJEON_AGGREGATOR_BOARD);
    }
}

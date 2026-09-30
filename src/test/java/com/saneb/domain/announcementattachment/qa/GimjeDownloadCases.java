package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GimjeDownloadCases {
    static ObservationCase selectCase() {
        String url = "https://www.gimje.go.kr/board/view.gimje?boardId=BBS_0000044&menuCd=DOM_000000104003000000&paging=ok&startPage=1&dataSid=310426";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("GIMJE-310426", "2026년 영세소상공인 카드수수료 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)), url,
                        "LGS-000169", "SUBJECT_NOTICE_TABLE"), new GimjeAttachmentDiscoveryProfile(),
                "https://www.gimje.go.kr/index.gimje?menuCd=DOM_000000104003000000", 2, TitleLayout.GIMJE_BOARD);
    }
}

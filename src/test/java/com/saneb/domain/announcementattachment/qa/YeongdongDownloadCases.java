package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class YeongdongDownloadCases {
    static ObservationCase selectCase() {
        String url = "https://www.yd21.go.kr/kr/html/sub02/020103.html?mode=V&no=759fdcd35933d6237c5cf16b4908416b&GotoPage=1";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("YEONGDONG-759FDCD3", "2026년 영동군 소상공인 이차보전금 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)), url,
                        "LGS-000141", "SAEOL_GOSI"), new YeongdongAttachmentDiscoveryProfile(),
                "https://www.yd21.go.kr/kr/html/sub02/020103.html?GotoPage=1&mode=L", 1, TitleLayout.YEONGDONG_BOARD);
    }
}

package com.saneb.domain.announcementattachment.qa;
import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
final class SejongDownloadCases {
    static ObservationCase selectCase() {
        var profile = new SejongAttachmentDiscoveryProfile();
        String url = "https://www.sejong.go.kr/prog/publicNotice/kor/sub02_030301/C1_1/view.do?not_ancmt_mgt_no=68219&pageIndex=1";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("SEJONG-68219", "'26년 세종특별자치시 소상공인자금 지원계획 변경공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)), url, "LGS-000083", "SAEOL_GOSI"),
                profile, "https://www.sejong.go.kr/prog/publicNotice/kor/sub02_030301/C1_1/list.do", 1, TitleLayout.SEJONG_BOARD);
    }
}

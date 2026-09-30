package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.HwaseongAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class HwaseongDownloadCases {
    static ObservationCase selectCase() {
        String url = "https://www.hscity.go.kr/www/gosi/BD_selectNoticeDetail.do?q_notAncmtMgtNo=141781";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("HWASEONG-141781","2026년 화성시 소상공인 자금지원사업",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url,"LGS-000088","SAFE_HWASEONG_LEGAL_NOTICE"),new HwaseongAttachmentDiscoveryProfile(),
                "https://www.hscity.go.kr/www/gosi/BD_notice.do",1,TitleLayout.HWASEONG_BOARD);
    }
}

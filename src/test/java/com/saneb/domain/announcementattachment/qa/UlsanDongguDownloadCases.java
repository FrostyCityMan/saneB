package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.UlsanDongguAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementsource.provider.content.UlsanDongguNoticePage;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class UlsanDongguDownloadCases {
    static ObservationCase selectCase() {
        return selectCase(false);
    }

    static ObservationCase selectCase(boolean cardSupport) {
        String endpoint = "https://" + UlsanDongguNoticePage.HOST + UlsanDongguNoticePage.DETAIL;
        String url = endpoint + "?context=NTIS&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                + "&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + (cardSupport ? "27835" : "28029")
                + "&not_ancmt_se_code=01%2C03%2C04%2C05&subCheck=Y";
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase(cardSupport ? "ULSAN_DONGGU-27835" : "ULSAN_DONGGU-28029",
                cardSupport ? "2026년 울산동구 소상공인 카드수수료 지원사업 공고" : "2026년 하반기 울산광역시동구 소상공인 경영안정자금 융자계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", normalizer.hash(normalizer.canonicalizeUrl(url)),
                        url, "LGS-000080", "DOBONG_NOTICE_TABLE"), new UlsanDongguAttachmentDiscoveryProfile(),
                endpoint, cardSupport ? 3 : 1, TitleLayout.ULSAN_DONGGU_BOARD);
    }
}

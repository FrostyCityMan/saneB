package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.GyeongbukProvinceAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.ObservationCase;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.TitleLayout;

final class GyeongbukProvinceDownloadCases {
    static ObservationCase selectCase() {
        var normalizer = new AnnouncementSourceIdentityNormalizer();
        String url = normalizer.canonicalizeUrl("https://www.gb.go.kr/page/10109/67.do?pageDtlOrdrNo=1&boardNo=1370252&boardMngNo=71&importUrl=%2Fboard%2Fview.do");
        return new ObservationCase("GYEONGBUK-PROVINCE-1370252","2026년 하반기 대학생 학자금대출 이자지원 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(url),url,"LGS-000200","SAEOL_GOSI"),
                new GyeongbukProvinceAttachmentDiscoveryProfile(),"https://www.gb.go.kr/Main/page.do?mnu_uid=6789&BD_CODE=gosi_notice",1,TitleLayout.GYEONGBUK_PROVINCE_BOARD);
    }
}

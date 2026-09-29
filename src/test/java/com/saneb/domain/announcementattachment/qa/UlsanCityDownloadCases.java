package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class UlsanCityDownloadCases {
    static ObservationCase selectCase() {
        String url="https://www.ulsan.go.kr/u/rep/transfer/notice/47059.ulsan?mId=001004002000000000&gosiGbn=A";
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("ULSAN_CITY-47059", "2026년 4차 소상공인 경영안정자금 융자지원계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000077","SAEOL_GOSI"),
                new UlsanCityAttachmentDiscoveryProfile(),"https://www.ulsan.go.kr/u/rep/contents.ulsan?mId=001004002000000000",1,TitleLayout.ULSAN_CITY_BOARD);
    }
}

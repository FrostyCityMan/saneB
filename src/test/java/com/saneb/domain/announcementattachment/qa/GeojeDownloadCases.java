package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GeojeDownloadCases {
    static ObservationCase selectCase(){
        String url="https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&m=D&idx=67219";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("GEOJE-67219","2026년「소상공인 디지털 인프라 지원사업」 계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000230","SAEOL_GOSI"),new GeojeAttachmentDiscoveryProfile(),
                "https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&startPage=1",1,TitleLayout.GEOJE_BOARD);
    }
}

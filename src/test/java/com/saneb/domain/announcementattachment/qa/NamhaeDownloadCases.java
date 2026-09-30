package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class NamhaeDownloadCases {
    static ObservationCase selectCase() {
        String url="https://www.namhae.go.kr/modules/saeol/gosi.do?amode=_view&not_ancmt_mgt_no=35694&scd=01&pageCd=SM010110000&siteGubun=socialm";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("NAMHAE-35694","2026년 남해군 소상공인 임대료 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000236","SCMS_CARD_NOTICE"),
                new NamhaeAttachmentDiscoveryProfile(),
                "https://www.namhae.go.kr/socialm/Index.do?c=SM010110000",1,TitleLayout.NAMHAE_BOARD);
    }
}

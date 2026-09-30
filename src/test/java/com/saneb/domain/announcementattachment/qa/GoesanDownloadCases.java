package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.SaeolGetAttachmentProfileConfiguration;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class GoesanDownloadCases {
    static ObservationCase selectCase() {
        String url="https://eminwon.goesan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do"
                +"?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt"
                +"&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=29655&subCheck=Y";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("GOESAN-29655","2026년도 괴산형 소상공인 육성자금 지원계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,
                        "LGS-000144","SAFE_SAEOL_EMINWON_CELL"),
                new SaeolGetAttachmentProfileConfiguration().selectGoesanProfileDetails(),
                "https://eminwon.goesan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",1,TitleLayout.GOESAN_BOARD);
    }
}

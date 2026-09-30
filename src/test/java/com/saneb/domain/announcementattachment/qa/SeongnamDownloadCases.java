package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SeongnamDownloadCases {
    static ObservationCase selectCase(){
        String url="https://eminwon.seongnam.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=144735&subCheck=Y";
        var n=new AnnouncementSourceIdentityNormalizer();return new ObservationCase("SEONGNAM-144735","2026년 성남시 소상공인 특례보증 계획 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000089","SAFE_SAEOL_EMINWON"),new SeongnamAttachmentProfileConfiguration().selectSeongnamProfileDetails(),
                "https://eminwon.seongnam.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01%2C02%2C03%2C04%2C05%2C06%2C07",1,TitleLayout.SEONGNAM_BOARD);
    }
}

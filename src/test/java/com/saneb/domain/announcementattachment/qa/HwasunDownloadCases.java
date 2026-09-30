package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class HwasunDownloadCases {
    static ObservationCase selectCase() {
        String url="https://eminwon.hwasun.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&subCheck=Y&not_ancmt_mgt_no=39230";
        var n=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("HWASUN-39230","2026년 전남광주 청년 문화복지카드 지원사업 3차 모집 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",n.hash(n.canonicalizeUrl(url)),url,"LGS-000188","SAFE_SAEOL_EMINWON"),
                new SaeolGetAttachmentProfileConfiguration().selectHwasunProfileDetails(),
                "https://eminwon.hwasun.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",1,TitleLayout.HAMAN_LABEL);
    }
}

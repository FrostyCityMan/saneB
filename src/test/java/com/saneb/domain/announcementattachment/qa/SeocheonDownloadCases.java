package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.SeocheonAttachmentDiscoveryProfile;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;

final class SeocheonDownloadCases {
    static ObservationCase selectCase(){
        String url="https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=27490&subCheck=Y";
        var normalizer=new AnnouncementSourceIdentityNormalizer();
        return new ObservationCase("SEOCHEON-27490","2021년 서천군 소상공인 특례보증 지원사업 공고",
                new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE",normalizer.hash(normalizer.canonicalizeUrl(url)),url,"LGS-000158","SAFE_SAEOL_EMINWON_CELL"),
                new SeocheonAttachmentDiscoveryProfile(),"https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do",1,TitleLayout.SEOCHEON_BOARD);
    }
}

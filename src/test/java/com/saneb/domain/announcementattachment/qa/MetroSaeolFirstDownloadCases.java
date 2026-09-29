package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.*;
import com.saneb.domain.announcementsource.localgov.support.AnnouncementSourceIdentityNormalizer;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.*;
import java.util.Set;

final class MetroSaeolFirstDownloadCases {
    static final Set<String> GROUPS = Set.of("DAEGU_NAMGU", "DAEGU_BUKGU");
    static ObservationCase selectCase(String group) {
        if (!GROUPS.contains(group)) throw new IllegalArgumentException("GROUP_REQUIRED");
        boolean nam = group.equals("DAEGU_NAMGU"); var c = new MetroSaeolAttachmentProfileConfiguration();
        var p = nam ? c.selectDaeguNamguProfileDetails() : c.selectDaeguBukguProfileDetails();
        String id = nam ? "38607" : "55924", host = nam ? "eminwon.nam.daegu.kr" : "eminwon.buk.daegu.kr";
        String base = "https://" + host + "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do";
        String url = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=" + id + "&subCheck=Y";
        String list = base + "?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy=";
        String title = nam ? "대구광역시 남구 소상공인 경영안정자금 지원 사업 공고" : "청년창업 특례보증 지원사업 2차 변경 공고";
        var n = new AnnouncementSourceIdentityNormalizer(); var b = p.selectSourceBindings().getFirst();
        return new ObservationCase(group + "-" + id, title, new AttachmentDiscoveryProfile.Source("LOCAL_GOV_NOTICE", n.hash(n.canonicalizeUrl(url)), url,
                b.localSourceCode(), b.listParserProfileCode()), p, list, 1, nam ? TitleLayout.DAEGU_NAMGU_BOARD : TitleLayout.SEOCHO_BOARD);
    }
}
